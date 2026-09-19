package com.edgareldy.springjdbctutorial.core.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.auth.dao.RoleDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.UserDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.Role;
import com.edgareldy.springjdbctutorial.core.auth.entity.User;
import com.edgareldy.springjdbctutorial.core.auth.service.AuditLogger;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Unit tests of AdminBootstrap: DAOs, audit logger, encoder and transaction manager are mocks. The bootstrap
 * must create the first administrator only when configured, and must never throw whatever goes wrong.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    private static final String PASSWORD = "Dev-only-Passw0rd!";

    @Mock
    private UserDao userDao;
    @Mock
    private RoleDao roleDao;
    @Mock
    private AuditLogger audit;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        // A real TransactionTemplate over a mocked manager: the callback really runs, no database is needed
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    private AdminBootstrap bootstrap(String email, String password) {
        return new AdminBootstrap(userDao, roleDao, audit, passwordEncoder, transactionTemplate, email, password);
    }

    @Test
    void _01_ShouldDoNothing_WhenBothPropertiesAreAbsent() {
        bootstrap(null, null).run();

        verifyNoInteractions(userDao, roleDao, audit, passwordEncoder, transactionManager);
    }

    @Test
    void _02_ShouldDoNothing_WhenOnlyOneOfTheTwoPropertiesIsSetOrBlank() {
        bootstrap("admin@example.com", null).run();
        bootstrap(null, PASSWORD).run();
        bootstrap("   ", PASSWORD).run();
        bootstrap("admin@example.com", "").run();

        verifyNoInteractions(userDao, roleDao, audit, passwordEncoder, transactionManager);
    }

    @Test
    void _03_ShouldCreateEnabledUnlockedAdminWithAdminRoleAndAuditRow_WhenConfiguredAndAbsent() {
        when(userDao.existsByEmail("admin@example.com")).thenReturn(false);
        when(roleDao.findByRoleName("ADMIN")).thenReturn(Optional.of(new Role(1L, "ADMIN")));
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-hash");
        when(userDao.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(77L);
            return user;
        });

        bootstrap("  Admin@Example.COM ", PASSWORD).run();

        ArgumentCaptor<User> created = ArgumentCaptor.forClass(User.class);
        verify(userDao).insert(created.capture());
        assertThat(created.getValue().getEmail()).isEqualTo("admin@example.com");
        assertThat(created.getValue().getPassword()).isEqualTo("encoded-hash").isNotEqualTo(PASSWORD);
        assertThat(created.getValue().isEnabled()).isTrue();
        assertThat(created.getValue().isAccountLocked()).isFalse();
        verify(userDao).addRole(77L, 1L);
        verify(audit).log("BOOTSTRAP_ADMIN", "USER", 77L, "email=admin@example.com");
        verify(transactionManager).commit(any());
    }

    @Test
    void _04_ShouldSkipCreation_WhenTheUserAlreadyExists() {
        when(userDao.existsByEmail("admin@example.com")).thenReturn(true);

        bootstrap("admin@example.com", PASSWORD).run();

        verify(userDao, never()).insert(any());
        verify(userDao, never()).addRole(anyLong(), anyLong());
        verifyNoInteractions(audit, roleDao);
    }

    @Test
    void _05_ShouldSkipCreationWithoutThrowing_WhenTheAdminRoleIsMissing() {
        when(userDao.existsByEmail("admin@example.com")).thenReturn(false);
        when(roleDao.findByRoleName("ADMIN")).thenReturn(Optional.empty());

        assertThatCode(() -> bootstrap("admin@example.com", PASSWORD).run()).doesNotThrowAnyException();

        verify(userDao, never()).insert(any());
        verifyNoInteractions(audit);
        verify(transactionManager).rollback(any());
    }

    @Test
    void _06_ShouldSkipCreation_WhenThePasswordExceeds72Bytes() {
        bootstrap("admin@example.com", "a".repeat(73)).run();

        verifyNoInteractions(userDao, roleDao, audit, transactionManager);
    }

    @Test
    void _07_ShouldSkipCreation_WhenThePasswordIsUnder72CharactersButOver72Bytes() {
        // 40 characters, 80 bytes in UTF-8: bcrypt would silently ignore the tail, so the bootstrap refuses it
        bootstrap("admin@example.com", "é".repeat(40)).run();

        verifyNoInteractions(userDao, roleDao, audit, transactionManager);
    }

    @Test
    void _08_ShouldCreateTheAdmin_WhenThePasswordIsExactly72Bytes() {
        String limit = "a".repeat(72);
        when(userDao.existsByEmail("admin@example.com")).thenReturn(false);
        when(roleDao.findByRoleName("ADMIN")).thenReturn(Optional.of(new Role(1L, "ADMIN")));
        when(passwordEncoder.encode(limit)).thenReturn("encoded");
        when(userDao.insert(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bootstrap("admin@example.com", limit).run();

        verify(userDao).insert(any(User.class));
    }

    @Test
    void _09_ShouldNeverThrow_WhenTheDaoThrows() {
        when(userDao.existsByEmail("admin@example.com"))
                .thenThrow(new DataAccessResourceFailureException("database down"));

        assertThatCode(() -> bootstrap("admin@example.com", PASSWORD).run()).doesNotThrowAnyException();

        verify(userDao, never()).insert(any());
    }

    @Test
    void _10_ShouldNeverThrow_WhenInsertingTheUserFails() {
        when(userDao.existsByEmail("admin@example.com")).thenReturn(false);
        when(roleDao.findByRoleName("ADMIN")).thenReturn(Optional.of(new Role(1L, "ADMIN")));
        when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded");
        when(userDao.insert(any(User.class))).thenThrow(new IllegalStateException("boom"));

        assertThatCode(() -> bootstrap("admin@example.com", PASSWORD).run()).doesNotThrowAnyException();

        verify(userDao, never()).addRole(anyLong(), anyLong());
        verifyNoInteractions(audit);
    }

    @Test
    void _11_ShouldNeverThrow_WhenTheTransactionCannotBeStarted() {
        when(transactionManager.getTransaction(any())).thenThrow(new CannotCreateTransactionException("no connection"));

        assertThatCode(() -> bootstrap("admin@example.com", PASSWORD).run()).doesNotThrowAnyException();

        verifyNoInteractions(userDao, roleDao, audit);
    }

    @Test
    void _12_ShouldNeverThrow_WhenTheEncoderThrows() {
        when(userDao.existsByEmail("admin@example.com")).thenReturn(false);
        when(roleDao.findByRoleName("ADMIN")).thenReturn(Optional.of(new Role(1L, "ADMIN")));
        when(passwordEncoder.encode(PASSWORD)).thenThrow(new IllegalArgumentException("bad password"));

        assertThatCode(() -> bootstrap("admin@example.com", PASSWORD).run()).doesNotThrowAnyException();

        verify(userDao, never()).insert(any());
    }
}
