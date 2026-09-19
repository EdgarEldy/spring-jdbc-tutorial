package com.edgareldy.springjdbctutorial.core.auth.dao.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springjdbctutorial.core.auth.config.DaoConfig;
import com.edgareldy.springjdbctutorial.core.auth.dao.ActivationTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.ActivationToken;
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
 * Tests ActivationTokenDaoImpl against the real PostgreSQL, with its own fixture file
 * activation-token-dao-dataset.sql.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
@SpringJUnitConfig({DataSourceConfig.class, DaoConfig.class})
@Sql("/activation-token-dao-dataset.sql")
class ActivationTokenDaoImplTest {

    private static final String HASH_USER_1 = "a1".repeat(32);
    private static final String HASH_USER_2 = "b2".repeat(32);

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private ActivationTokenDao dao;

    @Test
    void _01_ShouldInsertAndGenerateId_WhenTokenIsNew() {
        Instant created = Instant.parse("2026-09-19T12:00:00Z");
        ActivationToken inserted = dao.insert(new ActivationToken(null, 1L, "9f".repeat(32), created,
                created.plusSeconds(3600), null));

        assertThat(inserted.getId()).isNotNull().isGreaterThanOrEqualTo(100L);
        ActivationToken reloaded = dao.findByTokenHash("9f".repeat(32)).orElseThrow();
        assertThat(reloaded.getUserId()).isEqualTo(1L);
        assertThat(reloaded.getCreatedAt()).isEqualTo(created);
        assertThat(reloaded.getExpiresAt()).isEqualTo(created.plusSeconds(3600));
        assertThat(reloaded.getValidatedAt()).isNull();
    }

    @Test
    void _02_ShouldFindToken_WhenHashExists() {
        ActivationToken token = dao.findByTokenHash(HASH_USER_2).orElseThrow();

        assertThat(token.getUserId()).isEqualTo(2L);
        assertThat(token.getValidatedAt()).isEqualTo(Instant.parse("2026-09-18T11:00:00Z"));
    }

    @Test
    void _03_ShouldReturnEmpty_WhenHashIsUnknown() {
        assertThat(dao.findByTokenHash("00".repeat(32))).isEmpty();
    }

    @Test
    void _04_ShouldStoreValidationDate_WhenTokenIsMarkedValidated() {
        Instant validatedAt = Instant.parse("2026-09-19T12:30:00Z");

        dao.markValidated(1L, validatedAt);

        assertThat(dao.findByTokenHash(HASH_USER_1).orElseThrow().getValidatedAt()).isEqualTo(validatedAt);
    }

    @Test
    void _05_ShouldDeleteOnlyThatUsersTokens_WhenDeletedByUserId() {
        dao.deleteByUserId(1L);

        assertThat(dao.findByTokenHash(HASH_USER_1)).isEmpty();
        assertThat(dao.findByTokenHash(HASH_USER_2)).isPresent();
    }
}
