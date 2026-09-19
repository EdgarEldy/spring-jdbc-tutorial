package com.edgareldy.springjdbctutorial.core.auth.mapper;

import com.edgareldy.springjdbctutorial.core.auth.entity.Role;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Converts one row of the roles table into a Role entity.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class RoleRowMapper implements RowMapper<Role> {

    @Override
    public Role mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Role(
                rs.getLong("id"),
                rs.getString("role_name"));
    }
}
