package com.edgareldy.springjdbctutorial.core.auth.dao.impl;

import com.edgareldy.springjdbctutorial.core.auth.dao.PasswordResetTokenDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.PasswordResetToken;
import com.edgareldy.springjdbctutorial.core.auth.mapper.PasswordResetTokenRowMapper;
import com.edgareldy.springjdbctutorial.core.common.dao.support.AbstractDao;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.Optional;

/**
 * JdbcTemplate implementation of PasswordResetTokenDao. Declared by @Bean in DaoConfig, no stereotype.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class PasswordResetTokenDaoImpl extends AbstractDao implements PasswordResetTokenDao {

    private final PasswordResetTokenRowMapper rowMapper = new PasswordResetTokenRowMapper();

    public PasswordResetTokenDaoImpl(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public PasswordResetToken insert(PasswordResetToken token) {
        long id = insertReturningId(
                "INSERT INTO password_reset_tokens (user_id, token, type, expiry_date) "
                        + "VALUES (?, ?, ?, ?) RETURNING id",
                token.getUserId(), token.getToken(), token.getType(), Timestamp.from(token.getExpiryDate()));
        token.setId(id);
        return token;
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return findOne("SELECT id, user_id, token, type, expiry_date FROM password_reset_tokens WHERE token = ?",
                rowMapper, tokenHash);
    }

    @Override
    public void deleteByUserId(Long userId) {
        getJdbcTemplate().update("DELETE FROM password_reset_tokens WHERE user_id = ?", userId);
    }
}
