package com.edgareldy.springjdbctutorial.core.order.mapper;

import com.edgareldy.springjdbctutorial.core.order.entity.Order;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converts one row of the orders table into an Order entity. Every column is NOT NULL.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class OrderRowMapper implements RowMapper<Order> {

    @Override
    public Order mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Order(
                rs.getLong("id"),
                rs.getLong("customer_id"),
                rs.getLong("product_id"),
                rs.getInt("quantity"),
                rs.getBigDecimal("total"));
    }
}
