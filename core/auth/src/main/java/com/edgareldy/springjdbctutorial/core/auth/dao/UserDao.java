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

    /** One page of users ordered by id (LIMIT/OFFSET). The page is zero-based. */
    List<User> findPage(int page, int size);

    long countAll();

    void addRole(Long userId, Long roleId);

    /** Returns the number of rows removed (0 when the role was not assigned). */
    int removeRole(Long userId, Long roleId);

    boolean hasRole(Long userId, Long roleId);

    /**
     * Number of distinct users that are enabled AND not locked and hold the ROLE:WRITE permission through
     * one of their roles. Reads database state, not live tokens.
     */
    long countLastAdminCandidates();

    /**
     * Takes a transaction-scoped advisory lock: must be the first statement of any transaction that could
     * remove the last ROLE:WRITE holder. Released automatically at commit or rollback.
     */
    void acquireLastAdminLock();
}
