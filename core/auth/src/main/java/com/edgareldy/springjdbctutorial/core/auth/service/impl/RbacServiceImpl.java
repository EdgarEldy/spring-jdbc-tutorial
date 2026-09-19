package com.edgareldy.springjdbctutorial.core.auth.service.impl;

import com.edgareldy.springjdbctutorial.core.auth.dao.PermissionDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.RoleDao;
import com.edgareldy.springjdbctutorial.core.auth.dao.UserDao;
import com.edgareldy.springjdbctutorial.core.auth.dto.PermissionDto;
import com.edgareldy.springjdbctutorial.core.auth.dto.RoleDto;
import com.edgareldy.springjdbctutorial.core.auth.dto.UserDto;
import com.edgareldy.springjdbctutorial.core.auth.entity.Permission;
import com.edgareldy.springjdbctutorial.core.auth.entity.Role;
import com.edgareldy.springjdbctutorial.core.auth.entity.User;
import com.edgareldy.springjdbctutorial.core.auth.mapper.PermissionMapper;
import com.edgareldy.springjdbctutorial.core.auth.mapper.RoleMapper;
import com.edgareldy.springjdbctutorial.core.auth.mapper.UserMapper;
import com.edgareldy.springjdbctutorial.core.auth.service.AuditLogger;
import com.edgareldy.springjdbctutorial.core.auth.service.RbacService;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implementation of RbacService. Declared by @Bean in ServiceConfig (no stereotype) and returned as the
 * interface so the @Transactional advice applies through a JDK proxy. The audit logger is a collaborator
 * bean called through its own proxy, which is what makes its REQUIRES_NEW refusal rows really independent.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public class RbacServiceImpl implements RbacService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_NAME_LENGTH = 100;
    private static final String LAST_ADMIN_MESSAGE = "Cannot remove the last account holding ROLE:WRITE";

    private static final String ROLE = "ROLE";
    private static final String PERMISSION = "PERMISSION";
    private static final String USER = "USER";

    private final UserDao userDao;
    private final RoleDao roleDao;
    private final PermissionDao permissionDao;
    private final AuditLogger audit;
    private final UserMapper userMapper = new UserMapper();
    private final RoleMapper roleMapper = new RoleMapper();
    private final PermissionMapper permissionMapper = new PermissionMapper();

    public RbacServiceImpl(UserDao userDao, RoleDao roleDao, PermissionDao permissionDao, AuditLogger audit) {
        this.userDao = userDao;
        this.roleDao = roleDao;
        this.permissionDao = permissionDao;
        this.audit = audit;
    }

    // ------------------------------------------------------------------ users

    @Override
    @Transactional(readOnly = true)
    public PageDto<UserDto> listUsers(int page, int size) {
        if (page < 0) {
            throw new BusinessRuleException("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessRuleException("Size must be between 1 and " + MAX_PAGE_SIZE);
        }
        List<UserDto> content = new ArrayList<>();
        for (User user : userDao.findPage(page, size)) {
            content.add(toUserDto(user));
        }
        return new PageDto<>(content, page, size, userDao.countAll());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        return toUserDto(requireUser(id));
    }

    @Override
    @Transactional
    public UserDto assignRoleToUser(Long userId, Long roleId) {
        User user = requireUser(userId);
        Role role = requireRole(roleId);
        String details = "role=" + role.getRoleName() + ", userId=" + userId;
        if (userDao.hasRole(userId, roleId)) {
            throw reject("ASSIGN_ROLE_TO_USER", USER, userId, details, "Role is already assigned to this user");
        }
        try {
            userDao.addRole(userId, roleId);
        } catch (DuplicateKeyException e) {
            // Lost a race against a concurrent identical assignment: the primary key decided
            throw reject("ASSIGN_ROLE_TO_USER", USER, userId, details, "Role is already assigned to this user", e);
        }
        audit.log("ASSIGN_ROLE_TO_USER", USER, userId, details);
        return toUserDto(user);
    }

    @Override
    @Transactional
    public UserDto removeRoleFromUser(Long userId, Long roleId) {
        // The advisory lock is the FIRST statement, before any lookup, so concurrent last-admin operations queue up
        userDao.acquireLastAdminLock();
        User user = requireUser(userId);
        Role role = requireRole(roleId);
        String details = "role=" + role.getRoleName() + ", userId=" + userId;
        long before = userDao.countLastAdminCandidates();
        if (userDao.removeRole(userId, roleId) == 0) {
            throw new ResourceNotFoundException("Role " + roleId + " is not assigned to user " + userId);
        }
        guardLastAdmin(before, "REMOVE_ROLE_FROM_USER", USER, userId, details);
        audit.log("REMOVE_ROLE_FROM_USER", USER, userId, details);
        return toUserDto(user);
    }

    // ------------------------------------------------------------------ roles

    @Override
    @Transactional(readOnly = true)
    public List<RoleDto> listRoles() {
        // One extra query per role: acceptable for the handful of roles of a tutorial application
        List<RoleDto> roles = new ArrayList<>();
        for (Role role : roleDao.findAll()) {
            roles.add(toRoleDto(role));
        }
        return roles;
    }

    @Override
    @Transactional
    public RoleDto createRole(RoleDto input) {
        String name = normalizeRoleName(input);
        String details = "roleName=" + name;
        if (roleDao.existsByRoleName(name)) {
            throw reject("CREATE_ROLE", ROLE, null, details, "Role name already exists");
        }
        Role created;
        try {
            created = roleDao.insert(new Role(null, name));
        } catch (DuplicateKeyException e) {
            throw reject("CREATE_ROLE", ROLE, null, details, "Role name already exists", e);
        }
        audit.log("CREATE_ROLE", ROLE, created.getId(), details);
        return toRoleDto(created);
    }

    @Override
    @Transactional
    public RoleDto updateRole(Long id, RoleDto input) {
        Role role = requireRole(id);
        String name = normalizeRoleName(input);
        String details = "roleName=" + role.getRoleName() + " -> " + name;
        if (!name.equals(role.getRoleName()) && roleDao.existsByRoleName(name)) {
            throw reject("UPDATE_ROLE", ROLE, id, details, "Role name already exists");
        }
        try {
            roleDao.updateName(id, name);
        } catch (DuplicateKeyException e) {
            throw reject("UPDATE_ROLE", ROLE, id, details, "Role name already exists", e);
        }
        role.setRoleName(name);
        audit.log("UPDATE_ROLE", ROLE, id, details);
        return toRoleDto(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = requireRole(id);
        String details = "roleName=" + role.getRoleName();
        if (roleDao.countUsersWithRole(id) > 0) {
            throw reject("DELETE_ROLE", ROLE, id, details, "Role is still assigned to users");
        }
        try {
            roleDao.delete(id);
        } catch (DataIntegrityViolationException e) {
            // A user was assigned the role after the check above: the foreign key refuses the delete
            throw reject("DELETE_ROLE", ROLE, id, details, "Role is still assigned to users", e);
        }
        audit.log("DELETE_ROLE", ROLE, id, details);
    }

    @Override
    @Transactional
    public RoleDto assignPermissionToRole(Long roleId, Long permissionId) {
        Role role = requireRole(roleId);
        Permission permission = requirePermission(permissionId);
        String details = "role=" + role.getRoleName() + ", permission=" + code(permission);
        if (roleDao.hasPermission(roleId, permissionId)) {
            throw reject("ASSIGN_PERMISSION_TO_ROLE", ROLE, roleId, details,
                    "Permission is already assigned to this role");
        }
        try {
            roleDao.addPermission(roleId, permissionId);
        } catch (DuplicateKeyException e) {
            throw reject("ASSIGN_PERMISSION_TO_ROLE", ROLE, roleId, details,
                    "Permission is already assigned to this role", e);
        }
        audit.log("ASSIGN_PERMISSION_TO_ROLE", ROLE, roleId, details);
        return toRoleDto(role);
    }

    @Override
    @Transactional
    public RoleDto removePermissionFromRole(Long roleId, Long permissionId) {
        userDao.acquireLastAdminLock();
        Role role = requireRole(roleId);
        Permission permission = requirePermission(permissionId);
        String details = "role=" + role.getRoleName() + ", permission=" + code(permission);
        long before = userDao.countLastAdminCandidates();
        if (roleDao.removePermission(roleId, permissionId) == 0) {
            throw new ResourceNotFoundException("Permission " + permissionId + " is not assigned to role " + roleId);
        }
        guardLastAdmin(before, "REMOVE_PERMISSION_FROM_ROLE", ROLE, roleId, details);
        audit.log("REMOVE_PERMISSION_FROM_ROLE", ROLE, roleId, details);
        return toRoleDto(role);
    }

    // ------------------------------------------------------------ permissions

    @Override
    @Transactional(readOnly = true)
    public List<PermissionDto> listPermissions() {
        List<PermissionDto> permissions = new ArrayList<>();
        for (Permission permission : permissionDao.findAll()) {
            permissions.add(permissionMapper.toDto(permission));
        }
        return permissions;
    }

    @Override
    @Transactional
    public PermissionDto createPermission(PermissionDto input) {
        Permission wanted = normalizePermission(input);
        String details = "permission=" + code(wanted);
        if (permissionDao.existsByResourceAndAction(wanted.getResource(), wanted.getAction())) {
            throw reject("CREATE_PERMISSION", PERMISSION, null, details, "Permission already exists");
        }
        Permission created;
        try {
            created = permissionDao.insert(wanted);
        } catch (DuplicateKeyException e) {
            throw reject("CREATE_PERMISSION", PERMISSION, null, details, "Permission already exists", e);
        }
        audit.log("CREATE_PERMISSION", PERMISSION, created.getId(), details);
        return permissionMapper.toDto(created);
    }

    @Override
    @Transactional
    public PermissionDto updatePermission(Long id, PermissionDto input) {
        // Renaming ROLE:WRITE itself would strip it from every holder, so the last-admin protocol applies
        userDao.acquireLastAdminLock();
        Permission current = requirePermission(id);
        Permission wanted = normalizePermission(input);
        String details = "permission=" + code(current) + " -> " + code(wanted);
        boolean changed = !current.getResource().equals(wanted.getResource())
                || !current.getAction().equals(wanted.getAction());
        if (changed && permissionDao.existsByResourceAndAction(wanted.getResource(), wanted.getAction())) {
            throw reject("UPDATE_PERMISSION", PERMISSION, id, details, "Permission already exists");
        }
        long before = userDao.countLastAdminCandidates();
        wanted.setId(id);
        try {
            permissionDao.update(wanted);
        } catch (DuplicateKeyException e) {
            throw reject("UPDATE_PERMISSION", PERMISSION, id, details, "Permission already exists", e);
        }
        guardLastAdmin(before, "UPDATE_PERMISSION", PERMISSION, id, details);
        audit.log("UPDATE_PERMISSION", PERMISSION, id, details);
        return permissionMapper.toDto(wanted);
    }

    @Override
    @Transactional
    public void deletePermission(Long id) {
        Permission permission = requirePermission(id);
        String details = "permission=" + code(permission);
        if (permissionDao.countRolesWithPermission(id) > 0) {
            throw reject("DELETE_PERMISSION", PERMISSION, id, details, "Permission is still assigned to roles");
        }
        try {
            permissionDao.delete(id);
        } catch (DataIntegrityViolationException e) {
            // A role received the permission after the check above: the foreign key refuses the delete
            throw reject("DELETE_PERMISSION", PERMISSION, id, details, "Permission is still assigned to roles", e);
        }
        audit.log("DELETE_PERMISSION", PERMISSION, id, details);
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Last-admin rule: compares the number of enabled, unlocked ROLE:WRITE holders before and after the change
     * and refuses when it dropped from at least one to zero. JDBC statements of one transaction see their own
     * uncommitted writes, so no flush is needed (there is no persistence context here). The count reads the
     * database state, not live tokens: permissions embedded in a JWT only change with the user's next token.
     * Throwing rolls the change back; the refusal row is written first, in its own transaction.
     */
    private void guardLastAdmin(long before, String action, String entityType, Long entityId, String details) {
        long after = userDao.countLastAdminCandidates();
        if (before > 0 && after == 0) {
            throw reject(action, entityType, entityId, details, LAST_ADMIN_MESSAGE);
        }
    }

    private BusinessRuleException reject(String action, String entityType, Long entityId, String details,
                                         String message) {
        return reject(action, entityType, entityId, details, message, null);
    }

    /** Audits the refusal (independent transaction) and builds the exception the caller must throw. */
    private BusinessRuleException reject(String action, String entityType, Long entityId, String details,
                                         String message, Throwable cause) {
        audit.logRejected(action, entityType, entityId, details + ", reason=" + message);
        return cause == null ? new BusinessRuleException(message) : new BusinessRuleException(message, cause);
    }

    private User requireUser(Long id) {
        return userDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    private Role requireRole(Long id) {
        return roleDao.findById(id).orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
    }

    private Permission requirePermission(Long id) {
        return permissionDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + id));
    }

    private UserDto toUserDto(User user) {
        UserDto dto = userMapper.toDto(user);
        dto.setRoles(userDao.findRoleNamesByUserId(user.getId()));
        dto.setPermissions(userDao.findPermissionCodesByUserId(user.getId()));
        return dto;
    }

    private RoleDto toRoleDto(Role role) {
        RoleDto dto = roleMapper.toDto(role);
        List<PermissionDto> permissions = new ArrayList<>();
        for (Permission permission : roleDao.findPermissionsByRoleId(role.getId())) {
            permissions.add(permissionMapper.toDto(permission));
        }
        dto.setPermissions(permissions);
        return dto;
    }

    private static String code(Permission permission) {
        return permission.getResource() + ":" + permission.getAction();
    }

    private static String normalizeRoleName(RoleDto input) {
        if (input == null || input.getRoleName() == null || input.getRoleName().isBlank()) {
            throw new BusinessRuleException("Role name is required");
        }
        String name = input.getRoleName().trim();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BusinessRuleException("Role name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        return name;
    }

    private static Permission normalizePermission(PermissionDto input) {
        if (input == null) {
            throw new BusinessRuleException("Permission is required");
        }
        return new Permission(null, normalizeToken(input.getResource(), "Resource"),
                normalizeToken(input.getAction(), "Action"));
    }

    private static String normalizeToken(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(label + " is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > MAX_NAME_LENGTH || !normalized.matches("[A-Z_]+")) {
            throw new BusinessRuleException(label + " must contain only letters A-Z and underscores");
        }
        return normalized;
    }
}
