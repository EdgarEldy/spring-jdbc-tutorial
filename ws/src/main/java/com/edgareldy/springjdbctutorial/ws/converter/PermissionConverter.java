package com.edgareldy.springjdbctutorial.ws.converter;

import com.edgareldy.springjdbctutorial.core.auth.dto.PermissionDto;
import com.edgareldy.springjdbctutorial.ws.payload.rbac.PermissionRequest;
import com.edgareldy.springjdbctutorial.ws.payload.rbac.PermissionResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts between the core PermissionDto and the permission HTTP payloads.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public final class PermissionConverter {

    private PermissionConverter() {
    }

    public static PermissionDto toDto(PermissionRequest request) {
        PermissionDto dto = new PermissionDto();
        dto.setResource(request.getResource());
        dto.setAction(request.getAction());
        return dto;
    }

    public static PermissionResponse toResponse(PermissionDto dto) {
        return new PermissionResponse(dto.getId(), dto.getResource(), dto.getAction());
    }

    public static List<PermissionResponse> toResponses(List<PermissionDto> dtos) {
        List<PermissionResponse> responses = new ArrayList<>();
        if (dtos != null) {
            for (PermissionDto dto : dtos) {
                responses.add(toResponse(dto));
            }
        }
        return responses;
    }
}
