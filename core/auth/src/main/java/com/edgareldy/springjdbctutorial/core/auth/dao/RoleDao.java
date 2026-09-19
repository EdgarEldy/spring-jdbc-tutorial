package com.edgareldy.springjdbctutorial.core.auth.dao;

import com.edgareldy.springjdbctutorial.core.auth.entity.Permission;
import com.edgareldy.springjdbctutorial.core.auth.entity.Role;

import java.util.List;
import java.util.Optional;

/**
 * Data access for roles and their role_permission assignments. Entity types only.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public interface RoleDao {

    /** All roles ordered by name. */
    List<Role> findAll();

    Optional<Role> findById(Long id);

    Optional<Role> findByRoleName(String roleName);

    boolean existsByRoleName(String roleName);

    /** Inserts the role and returns it with its generated id. */
    Role insert(Role role);

    void updateName(Long id, String roleName);

    /** Deletes the role (its role_permission rows go with it by cascade). */
    void delete(Long id);

    /** Number of users currently assigned this role. */
    long countUsersWithRole(Long roleId);

    /** Permissions of the role ordered by resource then action. */
    List<Permission> findPermissionsByRoleId(Long roleId);

    void addPermission(Long roleId, Long permissionId);

    /** Returns the number of rows removed (0 when the permission was not assigned). */
    int removePermission(Long roleId, Long permissionId);

    boolean hasPermission(Long roleId, Long permissionId);
}
