package com.edgareldy.springjdbctutorial.core.catalog.mapper;

import com.edgareldy.springjdbctutorial.core.catalog.entity.Product;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converts one row of the products table into a Product entity. NUMERIC is read with getBigDecimal to keep exact cents.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class ProductRowMapper implements RowMapper<Product> {

    @Override
    public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Product(
                rs.getLong("id"),
                rs.getLong("category_id"),
                rs.getString("product_name"),
                rs.getBigDecimal("unit_price"));
    }
}
