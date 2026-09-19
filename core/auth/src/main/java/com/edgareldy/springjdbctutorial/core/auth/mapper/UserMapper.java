package com.edgareldy.springjdbctutorial.core.auth.mapper;

import com.edgareldy.springjdbctutorial.core.auth.dto.UserDto;
import com.edgareldy.springjdbctutorial.core.auth.entity.User;

import java.util.ArrayList;

/**
 * Converts between the User entity and the UserDto. The password hash is never copied to a dto;
 * roles and permissions are filled by the service.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class UserMapper {

    /**
     * Entity to dto: password is always null, roles and permissions start empty.
     */
    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), null,
                user.isEnabled(), user.isAccountLocked(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Dto to entity: the caller supplies the bcrypt hash, the dto's raw password is never read here.
     */
    public User toEntity(UserDto dto, String passwordHash) {
        if (dto == null) {
            return null;
        }
        return new User(dto.getId(), dto.getFirstName(), dto.getLastName(), dto.getEmail(), passwordHash,
                dto.isEnabled(), dto.isAccountLocked());
    }
}
