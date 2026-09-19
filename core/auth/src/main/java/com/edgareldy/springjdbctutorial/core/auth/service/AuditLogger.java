package com.edgareldy.springjdbctutorial.core.auth.service;

/**
 * Writes the RBAC audit trail. A success row joins the business transaction, a refusal row is written independently so it survives the rollback.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public interface AuditLogger {

    /** Records a successful mutation, inside the caller's transaction (disappears if that transaction rolls back). */
    void log(String action, String entityType, Long entityId, String details);

    /** Records a refused operation in its own transaction, under the action prefixed with REJECTED_. */
    void logRejected(String action, String entityType, Long entityId, String details);
}
