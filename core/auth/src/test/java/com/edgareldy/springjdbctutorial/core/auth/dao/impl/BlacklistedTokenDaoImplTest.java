package com.edgareldy.springjdbctutorial.core.auth.dao.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springjdbctutorial.core.auth.config.DaoConfig;
import com.edgareldy.springjdbctutorial.core.auth.dao.BlacklistedTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.BlacklistedToken;
import com.edgareldy.springjdbctutorial.core.common.config.DataSourceConfig;
import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests BlacklistedTokenDaoImpl against the real PostgreSQL, with its own fixture file
 * blacklisted-token-dao-dataset.sql.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
@SpringJUnitConfig({DataSourceConfig.class, DaoConfig.class})
@Sql("/blacklisted-token-dao-dataset.sql")
class BlacklistedTokenDaoImplTest {

    private static final String HASH_EXPIRED_1 = "e5".repeat(32);
    private static final String HASH_EXPIRED_2 = "f6".repeat(32);
    private static final String HASH_LIVE = "a7".repeat(32);

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private BlacklistedTokenDao dao;

    @Test
    void _01_ShouldInsertAndGenerateId_WhenTokenIsNew() {
        Instant now = Instant.parse("2026-09-19T12:00:00Z");
        BlacklistedToken inserted = dao.insert(new BlacklistedToken(null, 1L, "1c".repeat(32), "jti-new", now,
                now.minusSeconds(60), now.plusSeconds(3600), null));

        assertThat(inserted.getId()).isNotNull().isGreaterThanOrEqualTo(100L);
        assertThat(dao.existsByTokenHash("1c".repeat(32))).isTrue();
    }

    @Test
    void _02_ShouldReturnTrue_WhenHashIsBlacklisted() {
        assertThat(dao.existsByTokenHash(HASH_LIVE)).isTrue();
    }

    @Test
    void _03_ShouldReturnFalse_WhenHashIsNotBlacklisted() {
        assertThat(dao.existsByTokenHash("00".repeat(32))).isFalse();
    }

    @Test
    void _04_ShouldThrowDuplicateKeyException_WhenSameJtiIsInsertedTwice() {
        Instant now = Instant.parse("2026-09-19T12:00:00Z");
        BlacklistedToken sameJti = new BlacklistedToken(null, 1L, "2d".repeat(32), "jti-live", now,
                now, now.plusSeconds(3600), null);

        assertThatThrownBy(() -> dao.insert(sameJti)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void _05_ShouldDeleteOnlyExpiredRowsAndReturnTheirCount_WhenPurgedBeforeAnInstant() {
        int deleted = dao.deleteExpiredBefore(Instant.parse("2026-09-19T00:00:00Z"));

        assertThat(deleted).isEqualTo(2);
        assertThat(dao.existsByTokenHash(HASH_EXPIRED_1)).isFalse();
        assertThat(dao.existsByTokenHash(HASH_EXPIRED_2)).isFalse();
        assertThat(dao.existsByTokenHash(HASH_LIVE)).isTrue();
    }

    @Test
    void _06_ShouldDeleteNothing_WhenNoRowIsExpiredBeforeTheInstant() {
        assertThat(dao.deleteExpiredBefore(Instant.parse("2026-09-01T00:00:00Z"))).isZero();
    }
}
