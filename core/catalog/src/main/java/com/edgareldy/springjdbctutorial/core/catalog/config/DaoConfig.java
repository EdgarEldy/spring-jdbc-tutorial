package com.edgareldy.springjdbctutorial.core.catalog.config;

import com.edgareldy.springjdbctutorial.core.catalog.dao.CategoryDao;
import com.edgareldy.springjdbctutorial.core.catalog.dao.ProductDao;
import com.edgareldy.springjdbctutorial.core.catalog.dao.impl.CategoryDaoImpl;
import com.edgareldy.springjdbctutorial.core.catalog.dao.impl.ProductDaoImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Declares the DAO beans of the catalog module, typed by interface (no stereotype on the implementations).
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
@Configuration
public class DaoConfig {

    @Bean
    public CategoryDao categoryDao(JdbcTemplate jdbcTemplate) {
        return new CategoryDaoImpl(jdbcTemplate);
    }

    @Bean
    public ProductDao productDao(JdbcTemplate jdbcTemplate) {
        return new ProductDaoImpl(jdbcTemplate);
    }
}
