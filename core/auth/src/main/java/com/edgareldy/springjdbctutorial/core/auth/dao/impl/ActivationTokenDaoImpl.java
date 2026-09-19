package com.edgareldy.springjdbctutorial.core.auth.dao.impl;

import com.edgareldy.springjdbctutorial.core.auth.dao.ActivationTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.ActivationToken;
import com.edgareldy.springjdbctutorial.core.auth.mapper.ActivationTokenRowMapper;
import com.edgareldy.springjdbctutorial.core.common.dao.support.AbstractDao;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * JdbcTemplate implementation of ActivationTokenDao. Declared by @Bean in DaoConfig, no stereotype.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class ActivationTokenDaoImpl extends AbstractDao implements ActivationTokenDao {

    private final ActivationTokenRowMapper rowMapper = new ActivationTokenRowMapper();

    public ActivationTokenDaoImpl(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public ActivationToken insert(ActivationToken token) {
        long id = insertReturningId(
                "INSERT INTO activation_tokens (user_id, token, created_at, expires_at, validated_at) "
                        + "VALUES (?, ?, ?, ?, ?) RETURNING id",
                token.getUserId(), token.getToken(), toTimestamp(token.getCreatedAt()),
                toTimestamp(token.getExpiresAt()), toTimestamp(token.getValidatedAt()));
        token.setId(id);
        return token;
    }

    @Override
    public Optional<ActivationToken> findByTokenHash(String tokenHash) {
        return findOne("SELECT id, user_id, token, created_at, expires_at, validated_at "
                + "FROM activation_tokens WHERE token = ?", rowMapper, tokenHash);
    }

    @Override
    public void markValidated(Long id, Instant validatedAt) {
        getJdbcTemplate().update("UPDATE activation_tokens SET validated_at = ? WHERE id = ?",
                toTimestamp(validatedAt), id);
    }

    @Override
    public void deleteByUserId(Long userId) {
        getJdbcTemplate().update("DELETE FROM activation_tokens WHERE user_id = ?", userId);
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }
}
