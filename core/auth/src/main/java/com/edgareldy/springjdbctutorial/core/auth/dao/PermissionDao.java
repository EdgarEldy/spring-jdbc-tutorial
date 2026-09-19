package com.edgareldy.springjdbctutorial.core.auth.dao;

import com.edgareldy.springjdbctutorial.core.auth.entity.Permission;

import java.util.List;
import java.util.Optional;

/**
 * Data access for permissions. Entity types only.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public interface PermissionDao {

    /** All permissions ordered by resource then action. */
    List<Permission> findAll();

    Optional<Permission> findById(Long id);

    boolean existsByResourceAndAction(String resource, String action);

    /** Inserts the permission and returns it with its generated id. */
    Permission insert(Permission permission);

    void update(Permission permission);

    void delete(Long id);

    /** Number of roles currently holding this permission. */
    long countRolesWithPermission(Long permissionId);
}
