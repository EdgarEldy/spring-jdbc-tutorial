package com.edgareldy.springjdbctutorial.core.common.dao.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springjdbctutorial.core.common.config.DataSourceConfig;
import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests HealthDaoImpl against the real PostgreSQL, with its own fixture file health-dao-dataset.sql.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
@SpringJUnitConfig(DataSourceConfig.class)
@Sql("/health-dao-dataset.sql")
class HealthDaoImplTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void _01_ShouldReturnTrue_WhenDatabaseAnswers() {
        assertThat(new HealthDaoImpl(jdbcTemplate).isDatabaseReachable()).isTrue();
    }
}
