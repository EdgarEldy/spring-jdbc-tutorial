package com.edgareldy.springjdbctutorial.core.common.config;

import com.edgareldy.springjdbctutorial.core.common.dao.HealthDao;
import com.edgareldy.springjdbctutorial.core.common.dao.impl.HealthDaoImpl;
import com.edgareldy.springjdbctutorial.core.common.service.HealthService;
import com.edgareldy.springjdbctutorial.core.common.service.impl.HealthServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Wiring of the common module: imports the infrastructure and declares the health DAO and service.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// @Import pulls DataSourceConfig into whichever context imports this class, so a module config only
// has to import what it directly needs. Beans are typed by their interface: callers never see the
// implementation class.
@Configuration
@Import(DataSourceConfig.class)
public class CommonConfig {

    @Bean
    public HealthDao healthDao(JdbcTemplate jdbcTemplate) {
        return new HealthDaoImpl(jdbcTemplate);
    }

    @Bean
    public HealthService healthService(HealthDao healthDao) {
        return new HealthServiceImpl(healthDao);
    }
}
