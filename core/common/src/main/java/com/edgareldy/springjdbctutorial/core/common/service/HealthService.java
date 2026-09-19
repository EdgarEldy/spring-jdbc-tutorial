package com.edgareldy.springjdbctutorial.core.common.service;

import com.edgareldy.springjdbctutorial.core.common.dto.HealthDto;

/**
 * Health check use case, exposed to the web layer as a public route.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public interface HealthService {

    /**
     * Reports the application and database status; never throws when the database is down.
     */
    HealthDto check();
}
