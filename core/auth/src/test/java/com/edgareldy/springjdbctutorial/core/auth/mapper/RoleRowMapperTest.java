package com.edgareldy.springjdbctutorial.core.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.auth.entity.Role;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/**
 * Tests RoleRowMapper against a mocked ResultSet, without any database.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class RoleRowMapperTest {

    private final RoleRowMapper mapper = new RoleRowMapper();

    // Mockito.mock(ResultSet.class) is a fake row whose getters return what the test stubs, column by
    // column: the test only checks the column-to-field mapping, with no database and no driver.
    @Test
    void _01_ShouldMapEveryColumn_WhenRowIsComplete() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(4L);
        when(rs.getString("role_name")).thenReturn("EDITOR");

        Role role = mapper.mapRow(rs, 0);

        assertThat(role.getId()).isEqualTo(4L);
        assertThat(role.getRoleName()).isEqualTo("EDITOR");
    }
}
