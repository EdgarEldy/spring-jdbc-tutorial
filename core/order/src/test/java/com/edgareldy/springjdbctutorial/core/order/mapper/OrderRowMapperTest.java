package com.edgareldy.springjdbctutorial.core.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.order.entity.Order;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/**
 * Tests OrderRowMapper against a mocked ResultSet, without any database.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
class OrderRowMapperTest {

    private final OrderRowMapper mapper = new OrderRowMapper();

    // Mockito.mock(ResultSet.class) is a fake row whose getters return what the test stubs, column by
    // column: the test only checks the column-to-field mapping, with no database and no driver.
    @Test
    void _01_ShouldMapEveryColumn_WhenRowIsComplete() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(7L);
        when(rs.getLong("customer_id")).thenReturn(3L);
        when(rs.getLong("product_id")).thenReturn(4L);
        when(rs.getInt("quantity")).thenReturn(3);
        when(rs.getBigDecimal("total")).thenReturn(new BigDecimal("59.97"));

        Order order = mapper.mapRow(rs, 0);

        assertThat(order).isEqualTo(new Order(7L, 3L, 4L, 3, new BigDecimal("59.97")));
    }

    @Test
    void _02_ShouldKeepTheExactDecimalScale_WhenTotalIsZero() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(8L);
        when(rs.getLong("customer_id")).thenReturn(1L);
        when(rs.getLong("product_id")).thenReturn(1L);
        when(rs.getInt("quantity")).thenReturn(1);
        when(rs.getBigDecimal("total")).thenReturn(new BigDecimal("0.00"));

        Order order = mapper.mapRow(rs, 0);

        assertThat(order.getTotal()).isEqualTo(new BigDecimal("0.00"));
        assertThat(order.getQuantity()).isEqualTo(1);
    }
}
