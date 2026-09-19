package com.edgareldy.springjdbctutorial.core.auth.service.impl;

import com.edgareldy.springjdbctutorial.core.auth.dao.AuditLogDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.AuditLog;
import com.edgareldy.springjdbctutorial.core.auth.service.ActorProvider;
import com.edgareldy.springjdbctutorial.core.auth.service.AuditLogger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Implementation of AuditLogger. Declared by @Bean in ServiceConfig (no stereotype) and returned as the interface so the @Transactional advice applies through a JDK proxy.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class AuditLoggerImpl implements AuditLogger {

    private static final String REJECTED_PREFIX = "REJECTED_";

    private final AuditLogDao auditLogDao;
    private final ActorProvider actorProvider;
    private final Clock clock;

    public AuditLoggerImpl(AuditLogDao auditLogDao, ActorProvider actorProvider, Clock clock) {
        this.auditLogDao = auditLogDao;
        this.actorProvider = actorProvider;
        this.clock = clock;
    }

    // Propagation.REQUIRED (the default) joins the transaction already open in RbacServiceImpl: the audit
    // row commits or rolls back together with the business change, so a success row can never describe
    // a change that was rolled back.
    @Override
    @Transactional
    public void log(String action, String entityType, Long entityId, String details) {
        insert(action, entityType, entityId, details);
    }

    // Propagation.REQUIRES_NEW suspends the caller's transaction and runs in a brand new one on another
    // connection, committed as soon as this method returns. A refusal makes the business transaction roll
    // back (BusinessRuleException), but this row is already committed and survives. It only works because
    // RbacServiceImpl calls this bean through its Spring proxy (the AuditLogger interface bean); a call on
    // "this" would bypass the proxy and the advice, and the row would be rolled back with the rest.
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRejected(String action, String entityType, Long entityId, String details) {
        insert(REJECTED_PREFIX + action, entityType, entityId, details);
    }

    private void insert(String action, String entityType, Long entityId, String details) {
        auditLogDao.insert(new AuditLog(null, actorProvider.currentUserId(), action, entityType, entityId,
                details, clock.instant()));
    }
}
