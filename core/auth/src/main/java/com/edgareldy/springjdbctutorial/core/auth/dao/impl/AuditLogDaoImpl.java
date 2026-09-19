package com.edgareldy.springjdbctutorial.core.auth.dao.impl;

import com.edgareldy.springjdbctutorial.core.auth.dao.AuditLogDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.AuditLog;
import com.edgareldy.springjdbctutorial.core.auth.mapper.AuditLogRowMapper;
import com.edgareldy.springjdbctutorial.core.common.dao.support.AbstractDao;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.List;

/**
 * JdbcTemplate implementation of AuditLogDao. Declared by @Bean in DaoConfig, no stereotype.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class AuditLogDaoImpl extends AbstractDao implements AuditLogDao {

    private static final String COLUMNS = "id, actor_user_id, action, entity_type, entity_id, details, created_at";

    private final AuditLogRowMapper rowMapper = new AuditLogRowMapper();

    public AuditLogDaoImpl(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public AuditLog insert(AuditLog auditLog) {
        long id = insertReturningId(
                "INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, details, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
                auditLog.getActorUserId(), auditLog.getAction(), auditLog.getEntityType(), auditLog.getEntityId(),
                auditLog.getDetails(),
                auditLog.getCreatedAt() == null ? null : Timestamp.from(auditLog.getCreatedAt()));
        auditLog.setId(id);
        return auditLog;
    }

    @Override
    public List<AuditLog> findAll() {
        return getJdbcTemplate().query("SELECT " + COLUMNS + " FROM audit_logs ORDER BY id", rowMapper);
    }
}
