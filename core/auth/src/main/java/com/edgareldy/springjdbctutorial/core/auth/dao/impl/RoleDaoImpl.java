package com.edgareldy.springjdbctutorial.core.auth.dao.impl;

import com.edgareldy.springjdbctutorial.core.auth.dao.RoleDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.Permission;
import com.edgareldy.springjdbctutorial.core.auth.entity.Role;
import com.edgareldy.springjdbctutorial.core.auth.mapper.PermissionRowMapper;
import com.edgareldy.springjdbctutorial.core.auth.mapper.RoleRowMapper;
import com.edgareldy.springjdbctutorial.core.common.dao.support.AbstractDao;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate implementation of RoleDao. Declared by @Bean in DaoConfig, no stereotype.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class RoleDaoImpl extends AbstractDao implements RoleDao {

    private final RoleRowMapper rowMapper = new RoleRowMapper();
    private final PermissionRowMapper permissionRowMapper = new PermissionRowMapper();

    public RoleDaoImpl(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public List<Role> findAll() {
        return getJdbcTemplate().query("SELECT id, role_name FROM roles ORDER BY role_name", rowMapper);
    }

    @Override
    public Optional<Role> findById(Long id) {
        return findOne("SELECT id, role_name FROM roles WHERE id = ?", rowMapper, id);
    }

    @Override
    public Optional<Role> findByRoleName(String roleName) {
        return findOne("SELECT id, role_name FROM roles WHERE role_name = ?", rowMapper, roleName);
    }

    @Override
    public boolean existsByRoleName(String roleName) {
        return count("SELECT COUNT(*) FROM roles WHERE role_name = ?", roleName) > 0;
    }

    @Override
    public Role insert(Role role) {
        long id = insertReturningId("INSERT INTO roles (role_name) VALUES (?) RETURNING id", role.getRoleName());
        role.setId(id);
        return role;
    }

    @Override
    public void updateName(Long id, String roleName) {
        getJdbcTemplate().update("UPDATE roles SET role_name = ? WHERE id = ?", roleName, id);
    }

    @Override
    public void delete(Long id) {
        getJdbcTemplate().update("DELETE FROM roles WHERE id = ?", id);
    }

    @Override
    public long countUsersWithRole(Long roleId) {
        return count("SELECT COUNT(*) FROM role_user WHERE role_id = ?", roleId);
    }

    @Override
    public List<Permission> findPermissionsByRoleId(Long roleId) {
        return getJdbcTemplate().query(
                "SELECT p.id, p.resource, p.action FROM permissions p "
                        + "JOIN role_permission rp ON rp.permission_id = p.id "
                        + "WHERE rp.role_id = ? ORDER BY p.resource, p.action",
                permissionRowMapper, roleId);
    }

    @Override
    public void addPermission(Long roleId, Long permissionId) {
        getJdbcTemplate().update("INSERT INTO role_permission (role_id, permission_id) VALUES (?, ?)",
                roleId, permissionId);
    }

    @Override
    public int removePermission(Long roleId, Long permissionId) {
        return getJdbcTemplate().update("DELETE FROM role_permission WHERE role_id = ? AND permission_id = ?",
                roleId, permissionId);
    }

    @Override
    public boolean hasPermission(Long roleId, Long permissionId) {
        return count("SELECT COUNT(*) FROM role_permission WHERE role_id = ? AND permission_id = ?",
                roleId, permissionId) > 0;
    }
}
