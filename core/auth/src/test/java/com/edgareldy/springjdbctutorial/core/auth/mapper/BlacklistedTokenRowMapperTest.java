package com.edgareldy.springjdbctutorial.core.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.auth.entity.BlacklistedToken;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Tests BlacklistedTokenRowMapper against a mocked ResultSet, including a NULL validated_at.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class BlacklistedTokenRowMapperTest {

    private static final Instant BLACKLISTED = Instant.parse("2026-09-19T10:30:00Z");
    private static final Instant CREATED = Instant.parse("2026-09-19T10:00:00Z");
    private static final Instant EXPIRES = Instant.parse("2026-09-19T11:00:00Z");

    private final BlacklistedTokenRowMapper mapper = new BlacklistedTokenRowMapper();

    private ResultSet baseRow() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(4L);
        when(rs.getLong("user_id")).thenReturn(9L);
        when(rs.getString("token")).thenReturn("hash");
        when(rs.getString("jti")).thenReturn("jti-1");
        when(rs.getTimestamp("blacklisted_at")).thenReturn(Timestamp.from(BLACKLISTED));
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(CREATED));
        when(rs.getTimestamp("expires_at")).thenReturn(Timestamp.from(EXPIRES));
        return rs;
    }

    @Test
    void _01_ShouldMapEveryColumn_WhenValidationDateIsSet() throws SQLException {
        ResultSet rs = baseRow();
        Instant validated = Instant.parse("2026-09-19T10:45:00Z");
        when(rs.getTimestamp("validated_at")).thenReturn(Timestamp.from(validated));

        BlacklistedToken token = mapper.mapRow(rs, 0);

        assertThat(token.getId()).isEqualTo(4L);
        assertThat(token.getUserId()).isEqualTo(9L);
        assertThat(token.getToken()).isEqualTo("hash");
        assertThat(token.getJti()).isEqualTo("jti-1");
        assertThat(token.getBlacklistedAt()).isEqualTo(BLACKLISTED);
        assertThat(token.getCreatedAt()).isEqualTo(CREATED);
        assertThat(token.getExpiresAt()).isEqualTo(EXPIRES);
        assertThat(token.getValidatedAt()).isEqualTo(validated);
    }

    @Test
    void _02_ShouldMapNullValidationDate_WhenColumnIsNull() throws SQLException {
        ResultSet rs = baseRow();
        when(rs.getTimestamp("validated_at")).thenReturn(null);

        assertThat(mapper.mapRow(rs, 0).getValidatedAt()).isNull();
    }
}
