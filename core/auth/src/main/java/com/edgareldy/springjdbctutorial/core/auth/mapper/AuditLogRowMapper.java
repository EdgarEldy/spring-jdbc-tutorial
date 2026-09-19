package com.edgareldy.springjdbctutorial.core.auth.mapper;

import com.edgareldy.springjdbctutorial.core.auth.entity.AuditLog;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converts one row of the audit_logs table into an AuditLog entity. Nullable columns stay null.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class AuditLogRowMapper implements RowMapper<AuditLog> {

    @Override
    public AuditLog mapRow(ResultSet rs, int rowNum) throws SQLException {
        // getLong returns 0 for SQL NULL, so nullable BIGINT columns are read as objects
        return new AuditLog(
                rs.getLong("id"),
                rs.getObject("actor_user_id", Long.class),
                rs.getString("action"),
                rs.getString("entity_type"),
                rs.getObject("entity_id", Long.class),
                rs.getString("details"),
                RowMapperSupport.instant(rs, "created_at"));
    }
}
