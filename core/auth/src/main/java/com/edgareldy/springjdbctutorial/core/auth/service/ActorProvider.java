package com.edgareldy.springjdbctutorial.core.auth.service;

/**
 * Tells the core services who is performing the current operation, for the audit trail. Implemented by the web layer from the authenticated principal; any context importing ServiceConfig must provide one.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public interface ActorProvider {

    /** Id of the authenticated user, or null when there is no authenticated actor (bootstrap, scheduled job). */
    Long currentUserId();
}
