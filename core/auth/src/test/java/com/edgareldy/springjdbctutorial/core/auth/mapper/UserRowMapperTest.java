package com.edgareldy.springjdbctutorial.core.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.auth.entity.User;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/**
 * Tests UserRowMapper against a mocked ResultSet, without any database.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class UserRowMapperTest {

    private final UserRowMapper mapper = new UserRowMapper();

    // A RowMapper only reads columns from the ResultSet it is given. Mockito.mock(ResultSet.class)
    // creates a fake one whose getters return what the test stubs, column by column: no database, no
    // driver, so the test only checks the column-to-field mapping.
    @Test
    void _01_ShouldMapEveryColumn_WhenRowIsComplete() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(7L);
        when(rs.getString("first_name")).thenReturn("Alice");
        when(rs.getString("last_name")).thenReturn("Martin");
        when(rs.getString("email")).thenReturn("alice@example.com");
        when(rs.getString("password")).thenReturn("hash");
        when(rs.getBoolean("enabled")).thenReturn(true);
        when(rs.getBoolean("account_locked")).thenReturn(false);

        User user = mapper.mapRow(rs, 0);

        assertThat(user.getId()).isEqualTo(7L);
        assertThat(user.getFirstName()).isEqualTo("Alice");
        assertThat(user.getLastName()).isEqualTo("Martin");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getPassword()).isEqualTo("hash");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAccountLocked()).isFalse();
    }

    @Test
    void _02_ShouldMapLockedAndDisabledFlags_WhenColumnsAreInverted() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBoolean("enabled")).thenReturn(false);
        when(rs.getBoolean("account_locked")).thenReturn(true);

        User user = mapper.mapRow(rs, 0);

        assertThat(user.isEnabled()).isFalse();
        assertThat(user.isAccountLocked()).isTrue();
    }
}
