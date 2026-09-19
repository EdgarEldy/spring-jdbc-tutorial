package com.edgareldy.springjdbctutorial.ws.converter;

import com.edgareldy.springjdbctutorial.core.auth.dto.RoleDto;
import com.edgareldy.springjdbctutorial.ws.payload.rbac.RoleRequest;
import com.edgareldy.springjdbctutorial.ws.payload.rbac.RoleResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts between the core RoleDto and the role HTTP payloads.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public final class RoleConverter {

    private RoleConverter() {
    }

    public static RoleDto toDto(RoleRequest request) {
        RoleDto dto = new RoleDto();
        dto.setRoleName(request.getRoleName());
        return dto;
    }

    public static RoleResponse toResponse(RoleDto dto) {
        return new RoleResponse(dto.getId(), dto.getRoleName(), PermissionConverter.toResponses(dto.getPermissions()));
    }

    public static List<RoleResponse> toResponses(List<RoleDto> dtos) {
        List<RoleResponse> responses = new ArrayList<>();
        for (RoleDto dto : dtos) {
            responses.add(toResponse(dto));
        }
        return responses;
    }
}
