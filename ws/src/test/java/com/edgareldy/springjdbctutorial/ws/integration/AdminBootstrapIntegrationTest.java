package com.edgareldy.springjdbctutorial.ws.integration;

import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertError;
import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import com.edgareldy.springjdbctutorial.ws.config.WebMvcConfig;
import com.edgareldy.springjdbctutorial.ws.support.AuthTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * The optional admin bootstrap on the REAL application context: with app.bootstrap.admin-email and
 * app.bootstrap.admin-password set, the context creates the administrator at startup (enabled, ADMIN role,
 * one BOOTSTRAP_ADMIN audit row without actor) who can then log in through HTTP and use the RBAC routes.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// @TestPropertySource adds the two bootstrap properties to the Environment of THIS context only (a separate
// cached context from the other integration tests, which run without them). The bootstrap bean runs once
// while the context starts, so the account already exists when the first test begins. The values are
// dummy development ones and must never be set in a real test/prod configuration.
@SpringJUnitWebConfig(WebMvcConfig.class)
@TestPropertySource(properties = {
        "app.bootstrap.admin-email=Bootstrap.Admin@Example.com",
        "app.bootstrap.admin-password=Dev-only-Passw0rd!"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminBootstrapIntegrationTest {

    private static final String EMAIL = "bootstrap.admin@example.com";
    private static final String PASSWORD = "Dev-only-Passw0rd!";

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // The bootstrap admin holds ROLE:WRITE and would count in the last-admin rule of the other classes
    // (their isolation helper would disable it, but leaving no trace is cleaner): remove it after the class.
    @AfterAll
    void removeTheBootstrapAdmin() {
        jdbcTemplate.update("DELETE FROM users WHERE LOWER(email) = ?", EMAIL);
    }

    @Test
    void _01_ShouldHaveCreatedAnEnabledUnlockedAdminWithTheAdminRoleAndAHashedPassword_WhenTheContextStarted() {
        List<String> roles = jdbcTemplate.queryForList("SELECT r.role_name FROM roles r "
                + "JOIN role_user ru ON ru.role_id = r.id JOIN users u ON u.id = ru.user_id "
                + "WHERE LOWER(u.email) = ?", String.class, EMAIL);
        Boolean enabled = jdbcTemplate.queryForObject("SELECT enabled FROM users WHERE LOWER(email) = ?", Boolean.class, EMAIL);
        Boolean locked = jdbcTemplate.queryForObject("SELECT account_locked FROM users WHERE LOWER(email) = ?", Boolean.class, EMAIL);
        String storedPassword = jdbcTemplate.queryForObject("SELECT password FROM users WHERE LOWER(email) = ?", String.class, EMAIL);

        assertThat(roles).containsExactly("ADMIN");
        assertThat(enabled).isTrue();
        assertThat(locked).isFalse();
        assertThat(storedPassword).isNotEqualTo(PASSWORD).startsWith("$2");
    }

    @Test
    void _02_ShouldLogInAndUseTheRbacRoutes_WhenTheBootstrapAdminPresentsTheConfiguredCredentials() throws Exception {
        try (AuthTestSupport support = new AuthTestSupport(mockMvc)) {
            String jwt = support.loginToken(EMAIL, PASSWORD);

            JsonNode me = assertSuccess(support.getAs(jwt, "/api/v1/auth/me"), 200);
            assertThat(me.get("data").get("roles").get(0).asText()).isEqualTo("ADMIN");
            assertThat(me.get("data").get("permissions")).hasSize(14);
            assertSuccess(support.getAs(jwt, "/api/v1/users"), 200);
            assertSuccess(support.getAs(jwt, "/api/v1/roles"), 200);
            assertError(support.login(EMAIL, "wrong-Passw0rd!"), 401);
        }
    }

    @Test
    void _03_ShouldHaveWrittenOneBootstrapAuditRowWithoutActor_WhenTheContextStarted() {
        Long rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_logs a JOIN users u ON u.id = a.entity_id "
                + "WHERE a.action = 'BOOTSTRAP_ADMIN' AND a.entity_type = 'USER' AND a.actor_user_id IS NULL "
                + "AND LOWER(u.email) = ?", Long.class, EMAIL);

        assertThat(rows).isEqualTo(1L);
    }
}
