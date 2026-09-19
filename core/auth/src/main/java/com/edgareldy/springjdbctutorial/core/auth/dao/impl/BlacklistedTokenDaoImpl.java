package com.edgareldy.springjdbctutorial.core.auth.dao.impl;

import com.edgareldy.springjdbctutorial.core.auth.dao.BlacklistedTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.BlacklistedToken;
import com.edgareldy.springjdbctutorial.core.common.dao.support.AbstractDao;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * JdbcTemplate implementation of BlacklistedTokenDao. Declared by @Bean in DaoConfig, no stereotype.
 * Uses no RowMapper: the blacklist is only ever probed for existence, inserted or purged.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class BlacklistedTokenDaoImpl extends AbstractDao implements BlacklistedTokenDao {

    public BlacklistedTokenDaoImpl(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public BlacklistedToken insert(BlacklistedToken token) {
        long id = insertReturningId(
                "INSERT INTO blacklisted_tokens (user_id, token, jti, blacklisted_at, created_at, expires_at, "
                        + "validated_at) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id",
                token.getUserId(), token.getToken(), token.getJti(), toTimestamp(token.getBlacklistedAt()),
                toTimestamp(token.getCreatedAt()), toTimestamp(token.getExpiresAt()),
                toTimestamp(token.getValidatedAt()));
        token.setId(id);
        return token;
    }

    @Override
    public boolean existsByTokenHash(String tokenHash) {
        return count("SELECT COUNT(*) FROM blacklisted_tokens WHERE token = ?", tokenHash) > 0;
    }

    @Override
    public int deleteExpiredBefore(Instant instant) {
        return getJdbcTemplate().update("DELETE FROM blacklisted_tokens WHERE expires_at < ?",
                Timestamp.from(instant));
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
