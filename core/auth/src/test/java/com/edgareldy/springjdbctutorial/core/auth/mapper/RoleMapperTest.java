package com.edgareldy.springjdbctutorial.core.auth.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springjdbctutorial.core.auth.dto.PermissionDto;
import com.edgareldy.springjdbctutorial.core.auth.dto.RoleDto;
import com.edgareldy.springjdbctutorial.core.auth.entity.Role;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests RoleMapper (entity to dto and back) on hand-built objects, no mocks.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class RoleMapperTest {

    private final RoleMapper mapper = new RoleMapper();

    @Test
    void _01_ShouldCopyIdAndNameWithEmptyPermissions_WhenConvertingEntityToDto() {
        RoleDto dto = mapper.toDto(new Role(3L, "EDITOR"));

        assertThat(dto.getId()).isEqualTo(3L);
        assertThat(dto.getRoleName()).isEqualTo("EDITOR");
        assertThat(dto.getPermissions()).isNotNull().isEmpty();
    }

    @Test
    void _02_ShouldReturnNull_WhenEntityIsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void _03_ShouldCopyIdAndNameAndIgnorePermissions_WhenConvertingDtoToEntity() {
        RoleDto dto = new RoleDto(3L, "EDITOR", List.of(new PermissionDto(1L, "USER", "READ")));

        Role role = mapper.toEntity(dto);

        assertThat(role.getId()).isEqualTo(3L);
        assertThat(role.getRoleName()).isEqualTo("EDITOR");
    }

    @Test
    void _04_ShouldReturnNull_WhenDtoIsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
