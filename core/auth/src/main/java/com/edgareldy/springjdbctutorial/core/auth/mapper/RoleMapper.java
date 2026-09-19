package com.edgareldy.springjdbctutorial.core.auth.mapper;

import com.edgareldy.springjdbctutorial.core.auth.dto.RoleDto;
import com.edgareldy.springjdbctutorial.core.auth.entity.Role;

import java.util.ArrayList;

/**
 * Converts between the Role entity and the RoleDto. Permissions are filled by the service.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class RoleMapper {

    /**
     * Entity to dto: permissions start empty.
     */
    public RoleDto toDto(Role role) {
        if (role == null) {
            return null;
        }
        return new RoleDto(role.getId(), role.getRoleName(), new ArrayList<>());
    }

    public Role toEntity(RoleDto dto) {
        if (dto == null) {
            return null;
        }
        return new Role(dto.getId(), dto.getRoleName());
    }
}
