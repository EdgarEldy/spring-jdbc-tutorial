package com.edgareldy.springjdbctutorial.core.common.dao;

/**
 * Data access contract for the health check (no table involved, only connectivity).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public interface HealthDao {

    /**
     * Runs a trivial query to check the database answers.
     *
     * @return true when the query succeeded
     */
    boolean isDatabaseReachable();
}
