package com.edgareldy.springjdbctutorial.core.order.config;

import com.edgareldy.springjdbctutorial.core.order.dao.OrderDao;
import com.edgareldy.springjdbctutorial.core.order.dao.impl.OrderDaoImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Declares the DAO beans of the order module, typed by interface (no stereotype on the implementations).
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
@Configuration
public class DaoConfig {

    @Bean
    public OrderDao orderDao(JdbcTemplate jdbcTemplate) {
        return new OrderDaoImpl(jdbcTemplate);
    }
}
