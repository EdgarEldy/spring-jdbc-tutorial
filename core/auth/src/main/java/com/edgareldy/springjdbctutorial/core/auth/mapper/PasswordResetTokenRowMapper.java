package com.edgareldy.springjdbctutorial.core.auth.mapper;

import com.edgareldy.springjdbctutorial.core.auth.entity.PasswordResetToken;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converts one row of the password_reset_tokens table into a PasswordResetToken entity.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class PasswordResetTokenRowMapper implements RowMapper<PasswordResetToken> {

    @Override
    public PasswordResetToken mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PasswordResetToken(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("token"),
                rs.getString("type"),
                RowMapperSupport.instant(rs, "expiry_date"));
    }
}
