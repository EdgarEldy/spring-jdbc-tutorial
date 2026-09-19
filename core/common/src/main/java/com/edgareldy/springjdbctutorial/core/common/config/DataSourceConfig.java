package com.edgareldy.springjdbctutorial.core.common.config;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * The only place where DataSource, Flyway, JdbcTemplate and the transaction manager are declared.
 * Settings come from the Spring Environment (system properties, environment variables) with no default
 * for anything sensitive, so startup fails clearly when one is missing.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// @Configuration marks a class whose @Bean methods declare Spring beans explicitly. This is how the
// whole project is wired (no component scanning of core classes, no auto-configuration): a bean's
// existence and wiring are visible by reading the config class of its module. Spring calls each
// @Bean method once and registers the returned object under the method name.
//
// @EnableTransactionManagement turns on annotation-driven transactions: for every bean that has
// @Transactional methods (the service implementations declared by @Bean in the ServiceConfig
// classes), Spring wraps the bean in a proxy that opens/commits/rolls back a transaction around the
// call. It looks up the PlatformTransactionManager bean declared below.
@Configuration
@EnableTransactionManagement
public class DataSourceConfig {

    private static final int DEFAULT_MAX_POOL_SIZE = 10;

    private final Environment environment;

    public DataSourceConfig(Environment environment) {
        this.environment = environment;
    }

    // HikariCP is a fast JDBC connection pool: connections are opened once and reused instead of
    // being created per request. getRequiredProperty throws IllegalStateException when a key is
    // missing, so a wrong deployment fails at startup instead of using a hidden default (never put a
    // default password here). The pool size is not a secret, so it has a default.
    // destroyMethod "close" shuts the pool down when the context closes.
    @Bean(destroyMethod = "close")
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        // The driver class is named explicitly: inside a WAR the PostgreSQL driver lives in WEB-INF/lib,
        // in the webapp classloader, and java.sql.DriverManager (which only auto-registers drivers seen
        // by the system classloader) fails with "No suitable driver". Hikari loads the named class
        // through the classloader of the application instead.
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl(environment.getRequiredProperty("app.datasource.url"));
        dataSource.setUsername(environment.getRequiredProperty("app.datasource.username"));
        dataSource.setPassword(environment.getRequiredProperty("app.datasource.password"));
        dataSource.setMaximumPoolSize(
                environment.getProperty("app.datasource.max-pool-size", Integer.class, DEFAULT_MAX_POOL_SIZE));
        return dataSource;
    }

    // Flyway applies the versioned SQL scripts of db/migration (the schema source of truth) to the
    // database. initMethod "migrate" runs them as soon as the bean is created. The beans that use the
    // database (JdbcTemplate below, hence every DAO) are declared @DependsOn("flyway"), so Spring
    // always creates and runs Flyway BEFORE any DAO can touch a table.
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }

    @Bean
    @DependsOn("flyway")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // DataSourceTransactionManager implements local JDBC transactions: it binds one Connection to the
    // current thread for the duration of a @Transactional method, so every JdbcTemplate call made in
    // it (through any DAO) shares the same transaction and commits or rolls back together.
    @Bean
    @DependsOn("flyway")
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
