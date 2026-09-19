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
}
