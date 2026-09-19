package com.edgareldy.springjdbctutorial.core.auth.mapper;

import com.edgareldy.springjdbctutorial.core.auth.entity.ActivationToken;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converts one row of the activation_tokens table into an ActivationToken entity (validated_at may be NULL).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class ActivationTokenRowMapper implements RowMapper<ActivationToken> {

    @Override
    public ActivationToken mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ActivationToken(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("token"),
                RowMapperSupport.instant(rs, "created_at"),
                RowMapperSupport.instant(rs, "expires_at"),
                RowMapperSupport.instant(rs, "validated_at"));
    }
}
