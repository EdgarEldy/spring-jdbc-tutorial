package com.edgareldy.springjdbctutorial.core.auth.dao.impl;

import com.edgareldy.springjdbctutorial.core.auth.dao.UserDao;
import com.edgareldy.springjdbctutorial.core.auth.entity.User;
import com.edgareldy.springjdbctutorial.core.auth.mapper.UserRowMapper;
import com.edgareldy.springjdbctutorial.core.common.dao.support.AbstractDao;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate implementation of UserDao. Declared by @Bean in DaoConfig, no stereotype.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class UserDaoImpl extends AbstractDao implements UserDao {

    // Arbitrary but fixed key of the PostgreSQL advisory lock guarding the last-admin rule
    private static final long LAST_ADMIN_LOCK_KEY = 726_001_001L;

    private static final String COLUMNS = "id, first_name, last_name, email, password, enabled, account_locked";

    private final UserRowMapper rowMapper = new UserRowMapper();

    public UserDaoImpl(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public Optional<User> findById(Long id) {
        return findOne("SELECT " + COLUMNS + " FROM users WHERE id = ?", rowMapper, id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return findOne("SELECT " + COLUMNS + " FROM users WHERE LOWER(email) = LOWER(?)", rowMapper, email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return count("SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?)", email) > 0;
    }

    @Override
    public User insert(User user) {
        long id = insertReturningId(
                "INSERT INTO users (first_name, last_name, email, password, enabled, account_locked) "
                        + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
                user.getFirstName(), user.getLastName(), user.getEmail(), user.getPassword(),
                user.isEnabled(), user.isAccountLocked());
        user.setId(id);
        return user;
    }

    @Override
    public void updatePassword(Long userId, String passwordHash) {
        getJdbcTemplate().update("UPDATE users SET password = ? WHERE id = ?", passwordHash, userId);
    }

    @Override
    public void updateEnabled(Long userId, boolean enabled) {
        getJdbcTemplate().update("UPDATE users SET enabled = ? WHERE id = ?", enabled, userId);
    }

    @Override
    public List<String> findRoleNamesByUserId(Long userId) {
        return getJdbcTemplate().queryForList(
                "SELECT r.role_name FROM roles r JOIN role_user ru ON ru.role_id = r.id "
                        + "WHERE ru.user_id = ? ORDER BY r.role_name",
                String.class, userId);
    }

    @Override
    public List<String> findPermissionCodesByUserId(Long userId) {
        return getJdbcTemplate().queryForList(
                "SELECT DISTINCT p.resource || ':' || p.action AS code FROM permissions p "
                        + "JOIN role_permission rp ON rp.permission_id = p.id "
                        + "JOIN role_user ru ON ru.role_id = rp.role_id "
                        + "WHERE ru.user_id = ? ORDER BY code",
                String.class, userId);
    }

    @Override
    public List<User> findPage(int page, int size) {
        return getJdbcTemplate().query(
                "SELECT " + COLUMNS + " FROM users ORDER BY id LIMIT ? OFFSET ?",
                rowMapper, size, (long) page * size);
    }

    @Override
    public long countAll() {
        return count("SELECT COUNT(*) FROM users");
    }

    @Override
    public void addRole(Long userId, Long roleId) {
        getJdbcTemplate().update("INSERT INTO role_user (role_id, user_id) VALUES (?, ?)", roleId, userId);
    }

    @Override
    public int removeRole(Long userId, Long roleId) {
        return getJdbcTemplate().update("DELETE FROM role_user WHERE role_id = ? AND user_id = ?", roleId, userId);
    }

    @Override
    public boolean hasRole(Long userId, Long roleId) {
        return count("SELECT COUNT(*) FROM role_user WHERE role_id = ? AND user_id = ?", roleId, userId) > 0;
    }

    @Override
    public long countLastAdminCandidates() {
        return count("SELECT COUNT(DISTINCT u.id) FROM users u "
                + "JOIN role_user ru ON ru.user_id = u.id "
                + "JOIN role_permission rp ON rp.role_id = ru.role_id "
                + "JOIN permissions p ON p.id = rp.permission_id "
                + "WHERE u.enabled = TRUE AND u.account_locked = FALSE "
                + "AND p.resource = 'ROLE' AND p.action = 'WRITE'");
    }

    @Override
    public void acquireLastAdminLock() {
        // pg_advisory_xact_lock is a transaction-scoped advisory lock: concurrent transactions asking for the
        // same key wait here, which serialises every operation that could remove the last admin. It is
        // released at commit or rollback (no explicit unlock). The function returns void, hence queryForList.
        getJdbcTemplate().queryForList("SELECT pg_advisory_xact_lock(?)", LAST_ADMIN_LOCK_KEY);
    }
}
