package com.edgareldy.springjdbctutorial.core.order.dao.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springjdbctutorial.core.common.config.DataSourceConfig;
import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import com.edgareldy.springjdbctutorial.core.order.config.DaoConfig;
import com.edgareldy.springjdbctutorial.core.order.dao.OrderDao;
import com.edgareldy.springjdbctutorial.core.order.entity.Order;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests OrderDaoImpl against the real PostgreSQL, with its own fixture file order-dao-dataset.sql.
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
@Sql("/order-dao-dataset.sql")
class OrderDaoImplTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private OrderDao orderDao;

    @Test
    void _01_ShouldReturnFirstPageOrderedById_WhenNoFilterIsGiven() {
        assertThat(orderDao.findPage(0, 3, null, null)).extracting(Order::getId).containsExactly(1L, 2L, 3L);
    }

    @Test
    void _02_ShouldApplyOffsetOfPageTimesSize_WhenPageIsOne() {
        assertThat(orderDao.findPage(1, 3, null, null)).extracting(Order::getId).containsExactly(4L, 5L);
    }

    @Test
    void _03_ShouldReturnEmptyList_WhenPageIsBeyondTheLastOne() {
        assertThat(orderDao.findPage(50, 10, null, null)).isEmpty();
    }

    @Test
    void _04_ShouldReturnOnlyThatCustomersOrders_WhenFilteringByCustomerId() {
        assertThat(orderDao.findPage(0, 10, 2L, null)).extracting(Order::getId).containsExactly(3L, 4L);
    }

    @Test
    void _05_ShouldReturnOnlyThatProductsOrders_WhenFilteringByProductId() {
        assertThat(orderDao.findPage(0, 10, null, 2L)).extracting(Order::getId).containsExactly(2L, 4L);
    }

    @Test
    void _06_ShouldCombineBothFilters_WhenCustomerIdAndProductIdAreGiven() {
        assertThat(orderDao.findPage(0, 10, 1L, 1L)).extracting(Order::getId).containsExactly(1L, 5L);
    }

    @Test
    void _07_ShouldPaginateInsideTheFilter_WhenFilterAndPageAreCombined() {
        assertThat(orderDao.findPage(1, 2, 1L, null)).extracting(Order::getId).containsExactly(5L);
    }

    @Test
    void _08_ShouldReturnEmptyList_WhenNoOrderMatchesTheFilters() {
        assertThat(orderDao.findPage(0, 10, 999L, null)).isEmpty();
        assertThat(orderDao.findPage(0, 10, null, 999L)).isEmpty();
        assertThat(orderDao.findPage(0, 10, 2L, 999L)).isEmpty();
    }

    @Test
    void _09_ShouldCountWithTheSameFilters_WhenCounting() {
        assertThat(orderDao.count(null, null)).isEqualTo(5L);
        assertThat(orderDao.count(1L, null)).isEqualTo(3L);
        assertThat(orderDao.count(null, 2L)).isEqualTo(2L);
        assertThat(orderDao.count(1L, 1L)).isEqualTo(2L);
        assertThat(orderDao.count(2L, 999L)).isZero();
    }

    @Test
    void _10_ShouldReturnEveryColumn_WhenIdExists() {
        Order order = orderDao.findById(2L).orElseThrow();

        assertThat(order).isEqualTo(new Order(2L, 1L, 2L, 3, new BigDecimal("59.97")));
    }

    @Test
    void _11_ShouldReturnEmpty_WhenIdDoesNotExist() {
        assertThat(orderDao.findById(999L)).isEmpty();
    }

    @Test
    void _12_ShouldGenerateIdAndKeepTheExactTotal_WhenOrderIsInserted() {
        Order created = orderDao.insert(new Order(null, 2L, 2L, 7, new BigDecimal("139.93")));

        assertThat(created.getId()).isNotNull().isGreaterThanOrEqualTo(100L);
        Order read = orderDao.findById(created.getId()).orElseThrow();
        assertThat(read).isEqualTo(created);
        assertThat(read.getTotal()).isEqualByComparingTo("139.93");
        assertThat(orderDao.count(null, null)).isEqualTo(6L);
    }

    @Test
    void _13_ShouldRoundTripTheLargestTotal_WhenTotalIsTheColumnMaximum() {
        Order created = orderDao.insert(new Order(null, 1L, 1L, 1, new BigDecimal("999999999999.99")));

        assertThat(orderDao.findById(created.getId()).orElseThrow().getTotal()).isEqualByComparingTo("999999999999.99");
    }

    @Test
    void _14_ShouldThrowDataIntegrityViolationException_WhenCustomerDoesNotExist() {
        assertThatThrownBy(() -> orderDao.insert(new Order(null, 999L, 1L, 1, new BigDecimal("10.00"))))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(orderDao.count(null, null)).isEqualTo(5L);
    }

    @Test
    void _15_ShouldThrowDataIntegrityViolationException_WhenProductDoesNotExist() {
        assertThatThrownBy(() -> orderDao.insert(new Order(null, 1L, 999L, 1, new BigDecimal("10.00"))))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(orderDao.count(null, null)).isEqualTo(5L);
    }

    @Test
    void _16_ShouldThrowDataIntegrityViolationException_WhenQuantityIsNotPositive() {
        assertThatThrownBy(() -> orderDao.insert(new Order(null, 1L, 1L, 0, new BigDecimal("0.00"))))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(orderDao.count(null, null)).isEqualTo(5L);
    }
}
