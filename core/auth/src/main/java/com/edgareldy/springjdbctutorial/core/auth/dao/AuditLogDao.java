package com.edgareldy.springjdbctutorial.core.auth.dao;

import com.edgareldy.springjdbctutorial.core.auth.entity.AuditLog;

import java.util.List;

/**
 * Data access for the audit trail. Entity types only.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public interface AuditLogDao {

    /** Inserts the entry and returns it with its generated id. */
    AuditLog insert(AuditLog auditLog);

    /** Every entry ordered by id, meant for tests and diagnostics. */
    List<AuditLog> findAll();
}
