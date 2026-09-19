package com.edgareldy.springjdbctutorial.core.common.service.impl;

import com.edgareldy.springjdbctutorial.core.common.dao.HealthDao;
import com.edgareldy.springjdbctutorial.core.common.dto.HealthDto;
import com.edgareldy.springjdbctutorial.core.common.service.HealthService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;

/**
 * Implementation of {@link HealthService}: DOWN when the database cannot be queried.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class HealthServiceImpl implements HealthService {

    // Spring's own logging facade (spring-jcl): core needs no logging dependency, ws chooses the backend
    private static final Log LOG = LogFactory.getLog(HealthServiceImpl.class);

    private final HealthDao healthDao;

    public HealthServiceImpl(HealthDao healthDao) {
        this.healthDao = healthDao;
    }

    @Override
    public HealthDto check() {
        try {
            if (healthDao.isDatabaseReachable()) {
                return new HealthDto("UP", "UP");
            }
        } catch (DataAccessException e) {
            // JdbcTemplate translates SQLException into DataAccessException: database unreachable.
            // Logged so an operator sees the cause (bad credentials, network, exhausted pool).
            LOG.warn("Database health check failed", e);
        }
        return new HealthDto("DOWN", "DOWN");
    }
}
