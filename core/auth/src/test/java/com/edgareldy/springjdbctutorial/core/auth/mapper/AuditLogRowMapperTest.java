package com.edgareldy.springjdbctutorial.core.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.auth.entity.AuditLog;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Tests AuditLogRowMapper against a mocked ResultSet, including the NULL actor and entity id cases.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class AuditLogRowMapperTest {

    private static final Instant CREATED = Instant.parse("2026-09-19T10:15:30Z");

    private final AuditLogRowMapper mapper = new AuditLogRowMapper();

    @Test
    void _01_ShouldMapEveryColumn_WhenRowIsComplete() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(11L);
        when(rs.getObject("actor_user_id", Long.class)).thenReturn(3L);
        when(rs.getString("action")).thenReturn("CREATE_ROLE");
        when(rs.getString("entity_type")).thenReturn("ROLE");
        when(rs.getObject("entity_id", Long.class)).thenReturn(9L);
        when(rs.getString("details")).thenReturn("roleName=EDITOR");
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(CREATED));

        AuditLog log = mapper.mapRow(rs, 0);

        assertThat(log.getId()).isEqualTo(11L);
        assertThat(log.getActorUserId()).isEqualTo(3L);
        assertThat(log.getAction()).isEqualTo("CREATE_ROLE");
        assertThat(log.getEntityType()).isEqualTo("ROLE");
        assertThat(log.getEntityId()).isEqualTo(9L);
        assertThat(log.getDetails()).isEqualTo("roleName=EDITOR");
        assertThat(log.getCreatedAt()).isEqualTo(CREATED);
    }

    // rs.getLong would return 0 for SQL NULL and hide the absence: the mapper must read nullable
    // BIGINT columns as objects, so an unstubbed getObject (null) has to give a null Long, not 0.
    @Test
    void _02_ShouldMapNullActorAndNullEntityId_WhenColumnsAreSqlNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(12L);
        when(rs.getString("action")).thenReturn("BOOTSTRAP_ADMIN");
        when(rs.getString("entity_type")).thenReturn("USER");
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(CREATED));

        AuditLog log = mapper.mapRow(rs, 0);

        assertThat(log.getActorUserId()).isNull();
        assertThat(log.getEntityId()).isNull();
        assertThat(log.getDetails()).isNull();
    }

    @Test
    void _03_ShouldMapNullInstant_WhenCreatedAtIsSqlNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(13L);

        assertThat(mapper.mapRow(rs, 0).getCreatedAt()).isNull();
    }
}
