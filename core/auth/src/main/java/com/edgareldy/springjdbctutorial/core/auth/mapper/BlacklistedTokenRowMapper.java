package com.edgareldy.springjdbctutorial.core.auth.mapper;

import com.edgareldy.springjdbctutorial.core.auth.entity.BlacklistedToken;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converts one row of the blacklisted_tokens table into a BlacklistedToken entity (validated_at may be NULL).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class BlacklistedTokenRowMapper implements RowMapper<BlacklistedToken> {

    @Override
    public BlacklistedToken mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new BlacklistedToken(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("token"),
                rs.getString("jti"),
                RowMapperSupport.instant(rs, "blacklisted_at"),
                RowMapperSupport.instant(rs, "created_at"),
                RowMapperSupport.instant(rs, "expires_at"),
                RowMapperSupport.instant(rs, "validated_at"));
    }
}
