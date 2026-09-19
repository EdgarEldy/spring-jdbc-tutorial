package com.edgareldy.springjdbctutorial.core.auth.dao.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springjdbctutorial.core.auth.config.DaoConfig;
import com.edgareldy.springjdbctutorial.core.auth.dao.UserDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.User;
import com.edgareldy.springjdbctutorial.core.common.config.DataSourceConfig;
import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests UserDaoImpl against the real PostgreSQL, with its own fixture file user-dao-dataset.sql.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// DAO tests run real SQL on a real PostgreSQL started by Testcontainers (Docker), migrated by Flyway
// exactly like production. @SpringJUnitConfig builds a Spring context from the two real configuration
// classes; @DynamicPropertySource injects the container's random JDBC url into the Environment because
// the port is only known once the container runs. @Sql executes this class's own fixture file before
// EACH test, so every test starts from the same known rows.
@SpringJUnitConfig({DataSourceConfig.class, DaoConfig.class})
@Sql("/user-dao-dataset.sql")
class UserDaoImplTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private UserDao userDao;

    @Test
    void _01_ShouldReturnUser_WhenIdExists() {
        User user = userDao.findById(1L).orElseThrow();

        assertThat(user.getFirstName()).isEqualTo("Alice");
        assertThat(user.getLastName()).isEqualTo("Martin");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAccountLocked()).isFalse();
    }

    @Test
    void _02_ShouldReturnEmpty_WhenIdDoesNotExist() {
        assertThat(userDao.findById(999L)).isEmpty();
    }

    @Test
    void _03_ShouldFindUser_WhenEmailDiffersOnlyByCase() {
        assertThat(userDao.findByEmail("ALICE@Example.COM")).get().extracting(User::getId).isEqualTo(1L);
    }

    @Test
    void _04_ShouldReturnEmpty_WhenEmailIsUnknown() {
        assertThat(userDao.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void _05_ShouldReturnTrue_WhenEmailExistsWhateverItsCase() {
        assertThat(userDao.existsByEmail("BOB@EXAMPLE.com")).isTrue();
    }

    @Test
    void _06_ShouldReturnFalse_WhenEmailDoesNotExist() {
        assertThat(userDao.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void _07_ShouldGenerateIdAndPersistRow_WhenUserIsInserted() {
        User created = userDao.insert(new User(null, "Dave", "Roux", "dave@example.com", "hash-dave", false, false));

        assertThat(created.getId()).isNotNull().isGreaterThanOrEqualTo(100L);
        User reloaded = userDao.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo("dave@example.com");
        assertThat(reloaded.getPassword()).isEqualTo("hash-dave");
        assertThat(reloaded.isEnabled()).isFalse();
    }

    @Test
    void _08_ShouldThrowDuplicateKeyException_WhenEmailDiffersOnlyByCaseFromAnExistingOne() {
        User duplicate = new User(null, "Alice", "Clone", "ALICE@EXAMPLE.COM", "hash-clone", false, false);

        assertThatThrownBy(() -> userDao.insert(duplicate)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void _09_ShouldStoreNewHash_WhenPasswordIsUpdated() {
        userDao.updatePassword(2L, "new-hash");

        assertThat(userDao.findById(2L).orElseThrow().getPassword()).isEqualTo("new-hash");
    }

    @Test
    void _10_ShouldEnableAccount_WhenEnabledIsUpdated() {
        userDao.updateEnabled(2L, true);

        assertThat(userDao.findById(2L).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    void _11_ShouldReturnRoleNamesSorted_WhenUserHasRoles() {
        assertThat(userDao.findRoleNamesByUserId(1L)).containsExactly("ADMIN", "EDITOR");
    }

    @Test
    void _12_ShouldReturnDistinctSortedPermissionCodes_WhenTwoRolesGrantTheSamePermission() {
        // ADMIN grants USER:READ, USER:WRITE, ROLE:READ and EDITOR grants USER:READ again
        assertThat(userDao.findPermissionCodesByUserId(1L)).containsExactly("ROLE:READ", "USER:READ", "USER:WRITE");
    }

    @Test
    void _13_ShouldReturnEmptyLists_WhenUserHasNoRole() {
        assertThat(userDao.findRoleNamesByUserId(2L)).isEmpty();
        assertThat(userDao.findPermissionCodesByUserId(2L)).isEmpty();
    }
}
