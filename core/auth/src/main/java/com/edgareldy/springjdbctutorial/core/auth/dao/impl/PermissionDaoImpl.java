package com.edgareldy.springjdbctutorial.core.auth.dao.impl;

import com.edgareldy.springjdbctutorial.core.auth.dao.PermissionDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.Permission;
import com.edgareldy.springjdbctutorial.core.auth.mapper.PermissionRowMapper;
import com.edgareldy.springjdbctutorial.core.common.dao.support.AbstractDao;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate implementation of PermissionDao. Declared by @Bean in DaoConfig, no stereotype.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class PermissionDaoImpl extends AbstractDao implements PermissionDao {

    private final PermissionRowMapper rowMapper = new PermissionRowMapper();

    public PermissionDaoImpl(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public List<Permission> findAll() {
        return getJdbcTemplate().query(
                "SELECT id, resource, action FROM permissions ORDER BY resource, action", rowMapper);
    }

    @Override
    public Optional<Permission> findById(Long id) {
        return findOne("SELECT id, resource, action FROM permissions WHERE id = ?", rowMapper, id);
    }

    @Override
    public boolean existsByResourceAndAction(String resource, String action) {
        return count("SELECT COUNT(*) FROM permissions WHERE resource = ? AND action = ?", resource, action) > 0;
    }

    @Override
    public Permission insert(Permission permission) {
        long id = insertReturningId(
                "INSERT INTO permissions (resource, action) VALUES (?, ?) RETURNING id",
                permission.getResource(), permission.getAction());
        permission.setId(id);
        return permission;
    }

    @Override
    public void update(Permission permission) {
        getJdbcTemplate().update("UPDATE permissions SET resource = ?, action = ? WHERE id = ?",
                permission.getResource(), permission.getAction(), permission.getId());
    }

    @Override
    public void delete(Long id) {
        getJdbcTemplate().update("DELETE FROM permissions WHERE id = ?", id);
    }

    @Override
    public long countRolesWithPermission(Long permissionId) {
        return count("SELECT COUNT(*) FROM role_permission WHERE permission_id = ?", permissionId);
    }
}
