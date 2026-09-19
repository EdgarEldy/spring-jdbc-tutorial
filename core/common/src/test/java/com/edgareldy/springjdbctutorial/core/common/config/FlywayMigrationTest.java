package com.edgareldy.springjdbctutorial.core.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Checks that Flyway builds the whole V1 schema, that its key constraints hold and that the V2 seed
 * (baseline permissions and the ADMIN role) is exactly what the README requires, on a real PostgreSQL.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// @SpringJUnitConfig starts a real Spring context from DataSourceConfig (Flyway migrates at startup)
// and injects beans into the test. @DynamicPropertySource feeds the datasource settings into the
// context Environment once the Testcontainers database is running.
@SpringJUnitConfig(DataSourceConfig.class)
class FlywayMigrationTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void _01_ShouldCreateEveryTableOfTheDataModel_WhenApplicationStarts() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tables).contains("users", "roles", "permissions", "role_user", "role_permission",
                "activation_tokens", "blacklisted_tokens", "password_reset_tokens", "audit_logs",
                "categories", "products", "customers", "orders");
    }

    @Test
    void _02_ShouldRejectDuplicateEmail_WhenOnlyTheCaseDiffers() {
        String insert = "INSERT INTO users (first_name, last_name, email, password) VALUES ('A', 'B', ?, 'x')";
        jdbcTemplate.update(insert, "Case.Test@Example.com");
        try {
            assertThatThrownBy(() -> jdbcTemplate.update(insert, "case.test@example.COM"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbcTemplate.update("DELETE FROM users WHERE LOWER(email) = 'case.test@example.com'");
        }
    }

    @Test
    void _03_ShouldRefuseCategoryDeletion_WhenAProductStillReferencesIt() {
        Long categoryId = jdbcTemplate.queryForObject(
                "INSERT INTO categories (category_name) VALUES ('fk-restrict-test') RETURNING id", Long.class);
        jdbcTemplate.update("INSERT INTO products (category_id, product_name, unit_price) VALUES (?, 'p', 1.00)",
                categoryId);
        try {
            assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM categories WHERE id = ?", categoryId))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            jdbcTemplate.update("DELETE FROM products WHERE category_id = ?", categoryId);
            jdbcTemplate.update("DELETE FROM categories WHERE id = ?", categoryId);
        }
    }

    @Test
    void _04_ShouldSeedExactlyFourteenPermissions_WhenV2HasRun() {
        List<String> codes = jdbcTemplate.queryForList(
                "SELECT resource || ':' || action FROM permissions ORDER BY resource, action", String.class);

        // Seven resources times READ and WRITE, and nothing else
        assertThat(codes).containsExactly(
                "CATEGORY:READ", "CATEGORY:WRITE", "CUSTOMER:READ", "CUSTOMER:WRITE", "ORDER:READ", "ORDER:WRITE",
                "PERMISSION:READ", "PERMISSION:WRITE", "PRODUCT:READ", "PRODUCT:WRITE", "ROLE:READ", "ROLE:WRITE",
                "USER:READ", "USER:WRITE");
    }

    @Test
    void _05_ShouldSeedOneAdminRoleHoldingAllFourteenPermissions_WhenV2HasRun() {
        List<String> roles = jdbcTemplate.queryForList("SELECT role_name FROM roles", String.class);
        Long granted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role_permission rp JOIN roles r ON r.id = rp.role_id "
                        + "WHERE r.role_name = 'ADMIN'", Long.class);
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM role_permission", Long.class);

        assertThat(roles).containsExactly("ADMIN");
        assertThat(granted).isEqualTo(14L);
        assertThat(total).isEqualTo(14L);
    }

    @Test
    void _06_ShouldNotSeedAnyUser_WhenV2HasRun() {
        Long users = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        Long assignments = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM role_user", Long.class);

        assertThat(users).isZero();
        assertThat(assignments).isZero();
    }
}
