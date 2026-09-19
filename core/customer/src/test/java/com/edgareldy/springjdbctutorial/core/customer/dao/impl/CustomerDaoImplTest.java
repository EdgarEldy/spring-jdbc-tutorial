package com.edgareldy.springjdbctutorial.core.customer.dao.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springjdbctutorial.core.common.config.DataSourceConfig;
import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import com.edgareldy.springjdbctutorial.core.customer.config.DaoConfig;
import com.edgareldy.springjdbctutorial.core.customer.dao.CustomerDao;
import com.edgareldy.springjdbctutorial.core.customer.entity.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests CustomerDaoImpl against the real PostgreSQL, with its own fixture file customer-dao-dataset.sql.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
// Testcontainers starts a throw-away PostgreSQL in Docker (shared by every test class of the JVM through
// PostgresTestContainer); @DynamicPropertySource injects its url into the Spring Environment before the context
// is built, so DataSourceConfig connects to it and Flyway migrates the REAL schema. @Sql runs this class's own
// fixture file before EACH test so every test starts from the same rows.
@SpringJUnitConfig({DataSourceConfig.class, DaoConfig.class})
@Sql("/customer-dao-dataset.sql")
class CustomerDaoImplTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private CustomerDao customerDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void _01_ShouldReturnEveryColumn_WhenIdExists() {
        Customer customer = customerDao.findById(1L).orElseThrow();

        assertThat(customer).isEqualTo(new Customer(1L, "Alice", "Martin", "+33 1 23 45", "alice@example.com", "1 Main Street"));
    }

    @Test
    void _02_ShouldReturnEmpty_WhenIdDoesNotExist() {
        assertThat(customerDao.findById(999L)).isEmpty();
    }

    @Test
    void _03_ShouldReadNullOptionalColumns_WhenTheyAreNullInTheRow() {
        Customer customer = customerDao.findById(2L).orElseThrow();

        assertThat(customer.getFirstName()).isEqualTo("Bob");
        assertThat(customer.getTelephone()).isNull();
        assertThat(customer.getEmail()).isNull();
        assertThat(customer.getAddress()).isNull();
    }

    @Test
    void _04_ShouldReturnFirstPageOrderedById_WhenPageIsZero() {
        assertThat(customerDao.findPage(0, 3)).extracting(Customer::getId).containsExactly(1L, 2L, 3L);
    }

    @Test
    void _05_ShouldApplyOffsetOfPageTimesSize_WhenPageIsOne() {
        assertThat(customerDao.findPage(1, 3)).extracting(Customer::getId).containsExactly(4L);
    }

    @Test
    void _06_ShouldReturnEmptyList_WhenPageIsBeyondTheLastOne() {
        assertThat(customerDao.findPage(50, 10)).isEmpty();
    }

    @Test
    void _07_ShouldCountEveryRow_WhenCountingAll() {
        assertThat(customerDao.countAll()).isEqualTo(4L);
    }

    @Test
    void _08_ShouldGenerateIdAndPersistEveryColumn_WhenCustomerIsInserted() {
        Customer created = customerDao.insert(new Customer(null, "Eve", "Adams", "555", "eve@example.com", "5 Road"));

        assertThat(created.getId()).isNotNull().isGreaterThanOrEqualTo(100L);
        assertThat(customerDao.findById(created.getId())).contains(created);
        assertThat(customerDao.countAll()).isEqualTo(5L);
    }

    @Test
    void _09_ShouldRoundTripNulls_WhenOptionalColumnsAreNullOnInsert() {
        Customer created = customerDao.insert(new Customer(null, "Zed", "Zulu", null, null, null));

        Customer read = customerDao.findById(created.getId()).orElseThrow();

        assertThat(read.getTelephone()).isNull();
        assertThat(read.getEmail()).isNull();
        assertThat(read.getAddress()).isNull();
    }

    @Test
    void _10_ShouldReturnOneAndChangeEveryColumn_WhenCustomerExists() {
        int affected = customerDao.update(new Customer(2L, "Robert", "Stone", "111", "rob@example.com", "2 Road"));

        assertThat(affected).isEqualTo(1);
        assertThat(customerDao.findById(2L)).contains(new Customer(2L, "Robert", "Stone", "111", "rob@example.com", "2 Road"));
    }

    @Test
    void _11_ShouldWriteNulls_WhenUpdatedOptionalColumnsAreNull() {
        customerDao.update(new Customer(1L, "Alice", "Martin", null, null, null));

        Customer read = customerDao.findById(1L).orElseThrow();

        assertThat(read.getTelephone()).isNull();
        assertThat(read.getEmail()).isNull();
        assertThat(read.getAddress()).isNull();
    }

    @Test
    void _12_ShouldReturnZeroAndChangeNothing_WhenUpdatingAnUnknownId() {
        int affected = customerDao.update(new Customer(999L, "Ghost", "Nobody", null, null, null));

        assertThat(affected).isZero();
        assertThat(customerDao.countAll()).isEqualTo(4L);
    }

    @Test
    void _13_ShouldDeleteCustomer_WhenNoOrderReferencesIt() {
        customerDao.delete(4L);

        assertThat(customerDao.findById(4L)).isEmpty();
        assertThat(customerDao.countAll()).isEqualTo(3L);
    }

    @Test
    void _14_ShouldThrowDataIntegrityViolationException_WhenAnOrderStillReferencesTheCustomer() {
        // The order endpoints do not exist yet: the referencing row is inserted straight through SQL
        jdbcTemplate.update("INSERT INTO orders (customer_id, product_id, quantity, total) VALUES (3, 1, 2, 25.00)");

        assertThatThrownBy(() -> customerDao.delete(3L)).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(customerDao.findById(3L)).isPresent();
    }
}
