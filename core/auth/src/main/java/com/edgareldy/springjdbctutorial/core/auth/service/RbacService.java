package com.edgareldy.springjdbctutorial.core.auth.service;

import com.edgareldy.springjdbctutorial.core.auth.dto.PermissionDto;
import com.edgareldy.springjdbctutorial.core.auth.dto.RoleDto;
import com.edgareldy.springjdbctutorial.core.auth.dto.UserDto;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;

import java.util.List;

/**
 * Users, roles and permissions administration (RBAC). Dto types at the boundary. Missing resources throw ResourceNotFoundException (not audited), refusals BusinessRuleException (audited as REJECTED_*). Every successful mutation is audited.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public interface RbacService {

    /** One page of users with roles and permissions filled. Page must be >= 0 and size 1..100. */
    PageDto<UserDto> listUsers(int page, int size);

    UserDto getUser(Long id);

    UserDto assignRoleToUser(Long userId, Long roleId);

    /** Refused when it would leave no enabled, unlocked account holding ROLE:WRITE. */
    UserDto removeRoleFromUser(Long userId, Long roleId);

    /** Every role with its permissions. */
    List<RoleDto> listRoles();

    RoleDto createRole(RoleDto role);

    /** Renames the role (only the name is read). */
    RoleDto updateRole(Long id, RoleDto role);

    /** Refused while any user still has the role. */
    void deleteRole(Long id);

    RoleDto assignPermissionToRole(Long roleId, Long permissionId);

    /** Refused when it would leave no enabled, unlocked account holding ROLE:WRITE. */
    RoleDto removePermissionFromRole(Long roleId, Long permissionId);

    List<PermissionDto> listPermissions();

    PermissionDto createPermission(PermissionDto permission);

    /** Refused when it would strip ROLE:WRITE from the last account holding it. */
    PermissionDto updatePermission(Long id, PermissionDto permission);

    /** Refused while any role still has the permission. */
    void deletePermission(Long id);
}
