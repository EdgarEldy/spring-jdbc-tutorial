package com.edgareldy.springjdbctutorial.core.auth.mapper;

import com.edgareldy.springjdbctutorial.core.auth.dto.PermissionDto;
import com.edgareldy.springjdbctutorial.core.auth.entity.Permission;

/**
 * Converts between the Permission entity and the PermissionDto.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class PermissionMapper {

    public PermissionDto toDto(Permission permission) {
        if (permission == null) {
            return null;
        }
        return new PermissionDto(permission.getId(), permission.getResource(), permission.getAction());
    }

    public Permission toEntity(PermissionDto dto) {
        if (dto == null) {
            return null;
        }
        return new Permission(dto.getId(), dto.getResource(), dto.getAction());
    }
}
