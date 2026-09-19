package com.edgareldy.springjdbctutorial.core.auth.dao;

import com.edgareldy.springjdbctutorial.core.auth.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * Data access for users and for the read-only role/permission lookups of a user. Entity types only.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public interface UserDao {

    Optional<User> findById(Long id);

    /** Case-insensitive lookup. */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Inserts the user and returns it with its generated id. */
    User insert(User user);

    void updatePassword(Long userId, String passwordHash);

    void updateEnabled(Long userId, boolean enabled);

    /** Names of the roles assigned to the user, sorted. */
    List<String> findRoleNamesByUserId(Long userId);

    /** Distinct "RESOURCE:ACTION" codes granted through the user's roles, sorted. */
    List<String> findPermissionCodesByUserId(Long userId);
}
