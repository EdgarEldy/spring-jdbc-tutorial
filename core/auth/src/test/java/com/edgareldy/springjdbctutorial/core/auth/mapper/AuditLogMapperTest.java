package com.edgareldy.springjdbctutorial.core.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springjdbctutorial.core.auth.dto.AuditLogDto;
import com.edgareldy.springjdbctutorial.core.auth.entity.AuditLog;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Tests AuditLogMapper (entity to dto and back) on hand-built objects, no mocks.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class AuditLogMapperTest {

    private static final Instant CREATED = Instant.parse("2026-09-19T10:15:30Z");

    private final AuditLogMapper mapper = new AuditLogMapper();

    @Test
    void _01_ShouldCopyEveryField_WhenConvertingEntityToDto() {
        AuditLogDto dto = mapper.toDto(new AuditLog(1L, 2L, "CREATE_ROLE", "ROLE", 3L, "details", CREATED));

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getActorUserId()).isEqualTo(2L);
        assertThat(dto.getAction()).isEqualTo("CREATE_ROLE");
        assertThat(dto.getEntityType()).isEqualTo("ROLE");
        assertThat(dto.getEntityId()).isEqualTo(3L);
        assertThat(dto.getDetails()).isEqualTo("details");
        assertThat(dto.getCreatedAt()).isEqualTo(CREATED);
    }

    @Test
    void _02_ShouldKeepNullActorAndNullEntityId_WhenConvertingEntityToDto() {
        AuditLogDto dto = mapper.toDto(new AuditLog(1L, null, "BOOTSTRAP_ADMIN", "USER", null, null, CREATED));

        assertThat(dto.getActorUserId()).isNull();
        assertThat(dto.getEntityId()).isNull();
    }

    @Test
    void _03_ShouldReturnNull_WhenEntityIsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void _04_ShouldCopyEveryField_WhenConvertingDtoToEntity() {
        AuditLog log = mapper.toEntity(new AuditLogDto(1L, 2L, "CREATE_ROLE", "ROLE", 3L, "details", CREATED));

        assertThat(log.getId()).isEqualTo(1L);
        assertThat(log.getActorUserId()).isEqualTo(2L);
        assertThat(log.getAction()).isEqualTo("CREATE_ROLE");
        assertThat(log.getEntityType()).isEqualTo("ROLE");
        assertThat(log.getEntityId()).isEqualTo(3L);
        assertThat(log.getDetails()).isEqualTo("details");
        assertThat(log.getCreatedAt()).isEqualTo(CREATED);
    }

    @Test
    void _05_ShouldReturnNull_WhenDtoIsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
