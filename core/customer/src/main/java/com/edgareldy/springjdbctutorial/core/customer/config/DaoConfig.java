package com.edgareldy.springjdbctutorial.core.customer.config;

import com.edgareldy.springjdbctutorial.core.customer.dao.CustomerDao;
import com.edgareldy.springjdbctutorial.core.customer.dao.impl.CustomerDaoImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Declares the DAO beans of the customer module, typed by interface (no stereotype on the implementations).
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
@Configuration
public class DaoConfig {

    @Bean
    public CustomerDao customerDao(JdbcTemplate jdbcTemplate) {
        return new CustomerDaoImpl(jdbcTemplate);
    }
}
