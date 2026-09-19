package com.edgareldy.springjdbctutorial.core.catalog.mapper;

import com.edgareldy.springjdbctutorial.core.catalog.entity.Category;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converts one row of the categories table into a Category entity.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class CategoryRowMapper implements RowMapper<Category> {

    @Override
    public Category mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Category(
                rs.getLong("id"),
                rs.getString("category_name"));
    }
}
