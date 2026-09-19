package com.edgareldy.springjdbctutorial.ws.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springjdbctutorial.core.auth.service.impl.AdminBootstrap;
import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import com.edgareldy.springjdbctutorial.ws.config.WebMvcConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.web.context.WebApplicationContext;

/**
 * A bootstrap that cannot succeed (password over 72 bytes, refused before anything is written) must not fail
 * the startup of the real application context: the context loads, the bean exists, no account is created.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// If AdminBootstrap threw, the initMethod would make the whole context fail and JUnit would report every
// test of this class as an error before running it: merely reaching the test body proves the startup survived.
@SpringJUnitWebConfig(WebMvcConfig.class)
@TestPropertySource(properties = {
        "app.bootstrap.admin-email=too.long@example.com",
        "app.bootstrap.admin-password=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
class AdminBootstrapFailureIntegrationTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void _01_ShouldStartTheContextAndCreateNoAccount_WhenTheConfiguredPasswordIsTooLong() {
        assertThat(context.getBean(AdminBootstrap.class)).isNotNull();

        Long users = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE LOWER(email) = 'too.long@example.com'", Long.class);
        assertThat(users).isZero();
    }
}
