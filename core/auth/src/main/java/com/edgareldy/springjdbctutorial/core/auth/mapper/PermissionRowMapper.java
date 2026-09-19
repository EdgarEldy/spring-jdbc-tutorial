package com.edgareldy.springjdbctutorial.core.auth.mapper;

import com.edgareldy.springjdbctutorial.core.auth.entity.Permission;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converts one row of the permissions table into a Permission entity.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class PermissionRowMapper implements RowMapper<Permission> {

    @Override
    public Permission mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Permission(
                rs.getLong("id"),
                rs.getString("resource"),
                rs.getString("action"));
    }
}
