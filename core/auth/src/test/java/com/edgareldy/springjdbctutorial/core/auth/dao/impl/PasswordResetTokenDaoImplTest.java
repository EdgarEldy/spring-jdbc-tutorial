package com.edgareldy.springjdbctutorial.core.auth.dao.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springjdbctutorial.core.auth.config.DaoConfig;
import com.edgareldy.springjdbctutorial.core.auth.dao.PasswordResetTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.PasswordResetToken;
import com.edgareldy.springjdbctutorial.core.common.config.DataSourceConfig;
import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests PasswordResetTokenDaoImpl against the real PostgreSQL, with its own fixture file
 * password-reset-token-dao-dataset.sql.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
@SpringJUnitConfig({DataSourceConfig.class, DaoConfig.class})
@Sql("/password-reset-token-dao-dataset.sql")
class PasswordResetTokenDaoImplTest {

    private static final String HASH_USER_1 = "c3".repeat(32);
    private static final String HASH_USER_2 = "d4".repeat(32);

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private PasswordResetTokenDao dao;

    @Test
    void _01_ShouldInsertAndGenerateId_WhenTokenIsNew() {
        Instant expiry = Instant.parse("2026-09-19T13:00:00Z");
        PasswordResetToken inserted = dao.insert(new PasswordResetToken(null, 2L, "8e".repeat(32),
                "PASSWORD_RESET", expiry));

        assertThat(inserted.getId()).isNotNull().isGreaterThanOrEqualTo(100L);
        PasswordResetToken reloaded = dao.findByTokenHash("8e".repeat(32)).orElseThrow();
        assertThat(reloaded.getUserId()).isEqualTo(2L);
        assertThat(reloaded.getType()).isEqualTo("PASSWORD_RESET");
        assertThat(reloaded.getExpiryDate()).isEqualTo(expiry);
    }

    @Test
    void _02_ShouldFindToken_WhenHashExists() {
        PasswordResetToken token = dao.findByTokenHash(HASH_USER_1).orElseThrow();

        assertThat(token.getUserId()).isEqualTo(1L);
        assertThat(token.getExpiryDate()).isEqualTo(Instant.parse("2026-09-19T11:00:00Z"));
    }

    @Test
    void _03_ShouldReturnEmpty_WhenHashIsUnknown() {
        assertThat(dao.findByTokenHash("00".repeat(32))).isEmpty();
    }

    @Test
    void _04_ShouldDeleteOnlyThatUsersTokens_WhenDeletedByUserId() {
        dao.deleteByUserId(1L);

        assertThat(dao.findByTokenHash(HASH_USER_1)).isEmpty();
        assertThat(dao.findByTokenHash(HASH_USER_2)).isPresent();
    }
}
