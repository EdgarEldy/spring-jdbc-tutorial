package com.edgareldy.springjdbctutorial.core.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.auth.entity.PasswordResetToken;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Tests PasswordResetTokenRowMapper against a mocked ResultSet, including a NULL expiry_date.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class PasswordResetTokenRowMapperTest {

    private final PasswordResetTokenRowMapper mapper = new PasswordResetTokenRowMapper();

    @Test
    void _01_ShouldMapEveryColumn_WhenRowIsComplete() throws SQLException {
        Instant expiry = Instant.parse("2026-09-19T11:00:00Z");
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(5L);
        when(rs.getLong("user_id")).thenReturn(9L);
        when(rs.getString("token")).thenReturn("hash");
        when(rs.getString("type")).thenReturn("PASSWORD_RESET");
        when(rs.getTimestamp("expiry_date")).thenReturn(Timestamp.from(expiry));

        PasswordResetToken token = mapper.mapRow(rs, 0);

        assertThat(token.getId()).isEqualTo(5L);
        assertThat(token.getUserId()).isEqualTo(9L);
        assertThat(token.getToken()).isEqualTo("hash");
        assertThat(token.getType()).isEqualTo("PASSWORD_RESET");
        assertThat(token.getExpiryDate()).isEqualTo(expiry);
    }

    @Test
    void _02_ShouldMapNullExpiry_WhenColumnIsNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp("expiry_date")).thenReturn(null);

        assertThat(mapper.mapRow(rs, 0).getExpiryDate()).isNull();
    }
}
