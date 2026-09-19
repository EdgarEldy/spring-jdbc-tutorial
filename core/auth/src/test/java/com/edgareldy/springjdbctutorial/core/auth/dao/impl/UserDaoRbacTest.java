package com.edgareldy.springjdbctutorial.core.auth.dao.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springjdbctutorial.core.auth.config.DaoConfig;
import com.edgareldy.springjdbctutorial.core.auth.dao.UserDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.User;
import com.edgareldy.springjdbctutorial.core.common.config.DataSourceConfig;
import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tests the RBAC additions of UserDaoImpl (paging, role assignment, last-admin count and lock) against the
 * real PostgreSQL, with its own fixture file user-rbac-dao-dataset.sql. The pre-existing UserDao methods are
 * covered by UserDaoImplTest and its own user-dao-dataset.sql.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
@SpringJUnitConfig({DataSourceConfig.class, DaoConfig.class})
@Sql("/user-rbac-dao-dataset.sql")
class UserDaoRbacTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private UserDao userDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;

    // ------------------------------------------------------------------ paging

    @Test
    void _01_ShouldReturnFirstUsersOrderedById_WhenPageIsZero() {
        assertThat(userDao.findPage(0, 4)).extracting(User::getId).containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    void _02_ShouldApplyOffsetOfPageTimesSize_WhenPageIsOne() {
        assertThat(userDao.findPage(1, 4)).extracting(User::getId).containsExactly(5L, 6L);
        assertThat(userDao.findPage(2, 2)).extracting(User::getId).containsExactly(5L, 6L);
    }

    @Test
    void _03_ShouldReturnEmptyList_WhenPageIsBeyondTheLastOne() {
        assertThat(userDao.findPage(50, 4)).isEmpty();
    }

    @Test
    void _04_ShouldCountEveryUser_WhenCountingAll() {
        assertThat(userDao.countAll()).isEqualTo(6L);
    }

    // ------------------------------------------------------------------ role assignment

    @Test
    void _05_ShouldReportAssignment_WhenCheckingHasRole() {
        assertThat(userDao.hasRole(1L, 1L)).isTrue();
        assertThat(userDao.hasRole(5L, 1L)).isFalse();
    }

    @Test
    void _06_ShouldAssignRole_WhenAddingIt() {
        userDao.addRole(5L, 3L);

        assertThat(userDao.hasRole(5L, 3L)).isTrue();
        assertThat(userDao.findRoleNamesByUserId(5L)).containsExactly("VIEWER");
    }

    @Test
    void _07_ShouldThrowDuplicateKeyException_WhenRoleIsAlreadyAssigned() {
        assertThatThrownBy(() -> userDao.addRole(1L, 1L)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void _08_ShouldThrowDataIntegrityViolationException_WhenRoleDoesNotExist() {
        assertThatThrownBy(() -> userDao.addRole(5L, 999L)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void _09_ShouldReturnOneThenZero_WhenRemovingTheSameRoleTwice() {
        assertThat(userDao.removeRole(4L, 3L)).isEqualTo(1);
        assertThat(userDao.removeRole(4L, 3L)).isZero();
        assertThat(userDao.hasRole(4L, 3L)).isFalse();
    }

    // ------------------------------------------------------------------ last-admin candidates

    @Test
    void _10_ShouldCountOnlyEnabledUnlockedHolders_WhenDisabledAndLockedHoldersExist() {
        // alice and frank only: bob is disabled, carol is locked, dave holds no ROLE:WRITE, erin no role
        assertThat(userDao.countLastAdminCandidates()).isEqualTo(2L);
    }

    @Test
    void _11_ShouldCountAUserOnce_WhenSeveralOfItsRolesGrantRoleWrite() {
        // alice holds ADMIN and EDITOR, both granting ROLE:WRITE: still one candidate
        userDao.updateEnabled(6L, false);

        assertThat(userDao.countLastAdminCandidates()).isEqualTo(1L);
    }

    @Test
    void _12_ShouldExcludeUser_WhenItIsDisabled() {
        userDao.updateEnabled(1L, false);

        assertThat(userDao.countLastAdminCandidates()).isEqualTo(1L);
    }

    @Test
    void _13_ShouldExcludeUser_WhenItIsLocked() {
        jdbcTemplate.update("UPDATE users SET account_locked = TRUE WHERE id = 6");

        assertThat(userDao.countLastAdminCandidates()).isEqualTo(1L);
    }

    @Test
    void _14_ShouldIncludeUser_WhenItIsGrantedARoleHoldingRoleWrite() {
        userDao.addRole(5L, 5L);

        assertThat(userDao.countLastAdminCandidates()).isEqualTo(3L);
    }

    @Test
    void _15_ShouldKeepUserCounted_WhenOnlyOneOfItsTwoRoleWriteRolesIsRemoved() {
        userDao.removeRole(1L, 1L);

        assertThat(userDao.countLastAdminCandidates()).isEqualTo(2L);
    }

    @Test
    void _16_ShouldDropToZero_WhenEveryRoleWritePathIsRemoved() {
        jdbcTemplate.update("DELETE FROM role_permission WHERE permission_id = 1");

        assertThat(userDao.countLastAdminCandidates()).isZero();
    }

    // ------------------------------------------------------------------ advisory lock

    @Test
    void _17_ShouldAcquireTheLockWithoutError_WhenCalledInsideATransaction() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        assertThatCode(() -> template.executeWithoutResult(status -> userDao.acquireLastAdminLock()))
                .doesNotThrowAnyException();
    }

    // A transaction-scoped advisory lock makes a second transaction wait until the first one ends.
    // The waiting transaction runs in another thread (each thread borrows its own pooled connection).
    @Test
    void _18_ShouldMakeASecondTransactionWait_WhenTheFirstOneHoldsTheLock() throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        AtomicReference<Future<?>> second = new AtomicReference<>();
        AtomicBoolean doneWhileLocked = new AtomicBoolean(true);
        try {
            template.executeWithoutResult(status -> {
                userDao.acquireLastAdminLock();
                second.set(executor.submit(() -> template.executeWithoutResult(s -> userDao.acquireLastAdminLock())));
                try {
                    Thread.sleep(700);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                doneWhileLocked.set(second.get().isDone());
            });

            // The first transaction has committed, which released the lock: the second one can finish now
            assertThat(doneWhileLocked).isFalse();
            second.get().get(10, TimeUnit.SECONDS);
            assertThat(second.get().isDone()).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }
}
