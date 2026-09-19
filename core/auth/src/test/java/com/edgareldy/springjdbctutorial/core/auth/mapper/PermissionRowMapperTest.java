package com.edgareldy.springjdbctutorial.core.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.auth.entity.Permission;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/**
 * Tests PermissionRowMapper against a mocked ResultSet, without any database.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class PermissionRowMapperTest {

    private final PermissionRowMapper mapper = new PermissionRowMapper();

    @Test
    void _01_ShouldMapEveryColumn_WhenRowIsComplete() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(8L);
        when(rs.getString("resource")).thenReturn("ROLE");
        when(rs.getString("action")).thenReturn("WRITE");

        Permission permission = mapper.mapRow(rs, 0);

        assertThat(permission.getId()).isEqualTo(8L);
        assertThat(permission.getResource()).isEqualTo("ROLE");
        assertThat(permission.getAction()).isEqualTo("WRITE");
    }
}
