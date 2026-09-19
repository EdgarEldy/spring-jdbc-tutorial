package com.edgareldy.springjdbctutorial.core.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springjdbctutorial.core.auth.dto.PermissionDto;
import com.edgareldy.springjdbctutorial.core.auth.entity.Permission;
import org.junit.jupiter.api.Test;

/**
 * Tests PermissionMapper (entity to dto and back) on hand-built objects, no mocks.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class PermissionMapperTest {

    private final PermissionMapper mapper = new PermissionMapper();

    @Test
    void _01_ShouldCopyEveryField_WhenConvertingEntityToDto() {
        PermissionDto dto = mapper.toDto(new Permission(5L, "ROLE", "WRITE"));

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getResource()).isEqualTo("ROLE");
        assertThat(dto.getAction()).isEqualTo("WRITE");
    }

    @Test
    void _02_ShouldReturnNull_WhenEntityIsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void _03_ShouldCopyEveryField_WhenConvertingDtoToEntity() {
        Permission permission = mapper.toEntity(new PermissionDto(5L, "ROLE", "WRITE"));

        assertThat(permission.getId()).isEqualTo(5L);
        assertThat(permission.getResource()).isEqualTo("ROLE");
        assertThat(permission.getAction()).isEqualTo("WRITE");
    }

    @Test
    void _04_ShouldReturnNull_WhenDtoIsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
