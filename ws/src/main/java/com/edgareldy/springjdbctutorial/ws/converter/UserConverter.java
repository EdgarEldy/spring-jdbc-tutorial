package com.edgareldy.springjdbctutorial.ws.converter;

import com.edgareldy.springjdbctutorial.core.auth.dto.UserDto;
import com.edgareldy.springjdbctutorial.ws.payload.auth.RegisterRequest;
import com.edgareldy.springjdbctutorial.ws.payload.auth.UserResponse;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.ws.payload.common.PageResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts between the core UserDto and the auth HTTP payloads.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public final class UserConverter {

    private UserConverter() {
    }

    public static UserDto toDto(RegisterRequest request) {
        UserDto dto = new UserDto();
        dto.setFirstName(request.getFirstName());
        dto.setLastName(request.getLastName());
        dto.setEmail(request.getEmail());
        dto.setPassword(request.getPassword());
        return dto;
    }

    public static UserResponse toResponse(UserDto dto) {
        return new UserResponse(dto.getId(), dto.getFirstName(), dto.getLastName(), dto.getEmail(),
                dto.isEnabled(), dto.getRoles(), dto.getPermissions());
    }

    public static PageResponse<UserResponse> toPageResponse(PageDto<UserDto> page) {
        List<UserResponse> content = new ArrayList<>();
        for (UserDto dto : page.getContent()) {
            content.add(toResponse(dto));
        }
        return PageResponse.of(content, page.getPage(), page.getSize(), page.getTotalElements());
    }
}
