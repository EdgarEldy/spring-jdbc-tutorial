package com.edgareldy.springjdbctutorial.core.common.dao.impl;

import com.edgareldy.springjdbctutorial.core.common.dao.HealthDao;
import com.edgareldy.springjdbctutorial.core.common.dao.support.AbstractDao;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JdbcTemplate implementation of {@link HealthDao}. Declared by @Bean in CommonConfig, no stereotype.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class HealthDaoImpl extends AbstractDao implements HealthDao {

    public HealthDaoImpl(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public boolean isDatabaseReachable() {
        return count("SELECT 1") == 1L;
    }
}
