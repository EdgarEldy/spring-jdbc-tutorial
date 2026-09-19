package com.edgareldy.springjdbctutorial.ws.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Fixtures of the RBAC integration tests, on top of AuthTestSupport: admin and simple users whose JWT comes from
 * a REAL login, custom roles, audit trail queries and ISOLATION of the global last-admin state. All tests of a
 * JVM share one database, and the last-admin rule counts every enabled, unlocked ROLE:WRITE holder, so a test
 * first calls isolateLastAdminState() (which disables the other holders) and close() restores everything:
 * the users and roles it created are deleted and the disabled holders are enabled again.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public final class RbacTestSupport implements AutoCloseable {

    /** Every role created by these tests starts with this prefix, which is how close() finds them. */
    public static final String ROLE_PREFIX = "it-";
    /** Every permission created by these tests has a resource starting with this prefix (no seeded resource does). */
    public static final String PERMISSION_PREFIX = "IT";

    /** A user created by the fixtures: its database id, email and the JWT of its last login (null if it cannot log in). */
    public static final class TestUser {
        private final long id;
        private final String email;
        private String jwt;

        TestUser(long id, String email, String jwt) {
            this.id = id;
            this.email = email;
            this.jwt = jwt;
        }

        public long getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public String getJwt() {
            return jwt;
        }
    }

    /** One row of audit_logs, reduced to what the tests assert. */
    public static final class AuditRow {
        private final String action;
        private final String entityType;
        private final Long entityId;
        private final Long actorUserId;
        private final String details;

        AuditRow(String action, String entityType, Long entityId, Long actorUserId, String details) {
            this.action = action;
            this.entityType = entityType;
            this.entityId = entityId;
            this.actorUserId = actorUserId;
            this.details = details;
        }

        public String getAction() {
            return action;
        }

        public String getEntityType() {
            return entityType;
        }

        public Long getEntityId() {
            return entityId;
        }

        public Long getActorUserId() {
            return actorUserId;
        }

        public String getDetails() {
            return details;
        }

        @Override
        public String toString() {
            return action + "/" + entityType + "/" + entityId + "/actor=" + actorUserId;
        }
    }

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbc;
    private final AuthTestSupport auth;
    // Cost 4 keeps user creation fast; the login endpoint verifies with whatever cost the hash carries
    private final String passwordHash = new BCryptPasswordEncoder(4).encode(AuthTestSupport.PASSWORD);
    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<Long> disabledHolderIds = new ArrayList<>();

    public RbacTestSupport(MockMvc mockMvc, JdbcTemplate jdbc) {
        this.mockMvc = mockMvc;
        this.jdbc = jdbc;
        this.auth = new AuthTestSupport(mockMvc);
    }

    // ------------------------------------------------------------------ isolation

    /**
     * Disables every enabled, unlocked ROLE:WRITE holder currently in the database, so the test controls exactly
     * who counts for the last-admin rule. close() enables them again.
     */
    public void isolateLastAdminState() {
        List<Long> holders = jdbc.queryForList(
                "SELECT DISTINCT u.id FROM users u JOIN role_user ru ON ru.user_id = u.id "
                        + "JOIN role_permission rp ON rp.role_id = ru.role_id "
                        + "JOIN permissions p ON p.id = rp.permission_id "
                        + "WHERE u.enabled = TRUE AND u.account_locked = FALSE "
                        + "AND p.resource = 'ROLE' AND p.action = 'WRITE'", Long.class);
        for (Long id : holders) {
            jdbc.update("UPDATE users SET enabled = FALSE WHERE id = ?", id);
            disabledHolderIds.add(id);
        }
    }

    /** Deletes what the test created and re-enables the holders that isolateLastAdminState() disabled. */
    @Override
    public void close() {
        try {
            for (Long id : createdUserIds) {
                // role_user, tokens and blacklist rows go with the user by cascade
                jdbc.update("DELETE FROM users WHERE id = ?", id);
            }
            jdbc.update("DELETE FROM roles WHERE role_name LIKE ?", ROLE_PREFIX + "%");
            jdbc.update("DELETE FROM role_permission WHERE permission_id IN "
                    + "(SELECT id FROM permissions WHERE resource LIKE ?)", PERMISSION_PREFIX + "%");
            jdbc.update("DELETE FROM permissions WHERE resource LIKE ?", PERMISSION_PREFIX + "%");
        } finally {
            for (Long id : disabledHolderIds) {
                jdbc.update("UPDATE users SET enabled = TRUE WHERE id = ?", id);
            }
            auth.close();
        }
    }

    // ------------------------------------------------------------------ users and roles

    public long adminRoleId() {
        return jdbc.queryForObject("SELECT id FROM roles WHERE role_name = 'ADMIN'", Long.class);
    }

    public long permissionId(String code) {
        return jdbc.queryForObject("SELECT id FROM permissions WHERE resource || ':' || action = ?", Long.class, code);
    }

    /** A role named it-... holding the given "RESOURCE:ACTION" permissions, inserted directly in the database. */
    public long createRole(String... permissionCodes) {
        long id = jdbc.queryForObject("INSERT INTO roles (role_name) VALUES (?) RETURNING id", Long.class,
                uniqueRoleName());
        for (String code : permissionCodes) {
            int rows = jdbc.update("INSERT INTO role_permission (role_id, permission_id) "
                    + "SELECT ?, id FROM permissions WHERE resource || ':' || action = ?", id, code);
            assertThat(rows).as("permission " + code + " exists").isEqualTo(1);
        }
        return id;
    }

    public static String uniqueRoleName() {
        return ROLE_PREFIX + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Inserts a user, optionally gives it a role, and logs it in through the REAL login endpoint when the account is
     * enabled and unlocked (so the JWT carries the permissions of the role at this very moment).
     */
    public TestUser createUser(boolean enabled, boolean locked, Long roleId) throws Exception {
        String email = AuthTestSupport.uniqueEmail("rbac");
        long id = jdbc.queryForObject("INSERT INTO users (first_name, last_name, email, password, enabled, "
                + "account_locked) VALUES ('Test', 'User', ?, ?, ?, ?) RETURNING id", Long.class,
                email, passwordHash, enabled, locked);
        createdUserIds.add(id);
        if (roleId != null) {
            jdbc.update("INSERT INTO role_user (role_id, user_id) VALUES (?, ?)", roleId, id);
        }
        String jwt = enabled && !locked ? auth.loginToken(email, AuthTestSupport.PASSWORD) : null;
        return new TestUser(id, email, jwt);
    }

    /** An enabled user holding the seeded ADMIN role (all 14 permissions), logged in. */
    public TestUser createAdmin() throws Exception {
        return createUser(true, false, adminRoleId());
    }

    /** An enabled user with no role at all (no permission), logged in. */
    public TestUser createSimpleUser() throws Exception {
        return createUser(true, false, null);
    }

    /** Logs the user in again and returns a NEW jwt built from its CURRENT roles. */
    public String freshToken(TestUser user) throws Exception {
        user.jwt = auth.loginToken(user.email, AuthTestSupport.PASSWORD);
        return user.jwt;
    }

    public void assignRoleInDb(long userId, long roleId) {
        jdbc.update("INSERT INTO role_user (role_id, user_id) VALUES (?, ?)", roleId, userId);
    }

    public long countLastAdminCandidates() {
        return jdbc.queryForObject("SELECT COUNT(DISTINCT u.id) FROM users u "
                + "JOIN role_user ru ON ru.user_id = u.id JOIN role_permission rp ON rp.role_id = ru.role_id "
                + "JOIN permissions p ON p.id = rp.permission_id WHERE u.enabled = TRUE AND u.account_locked = FALSE "
                + "AND p.resource = 'ROLE' AND p.action = 'WRITE'", Long.class);
    }

    // ------------------------------------------------------------------ audit trail

    /** Highest audit_logs id right now: rows created by the test have a greater id. */
    public long auditBaseline() {
        return jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM audit_logs", Long.class);
    }

    public List<AuditRow> auditSince(long baseline) {
        return jdbc.query("SELECT action, entity_type, entity_id, actor_user_id, details FROM audit_logs "
                        + "WHERE id > ? ORDER BY id",
                (rs, n) -> new AuditRow(rs.getString("action"), rs.getString("entity_type"),
                        rs.getObject("entity_id", Long.class), rs.getObject("actor_user_id", Long.class),
                        rs.getString("details")),
                baseline);
    }

    public List<AuditRow> auditSince(long baseline, String action) {
        return auditSince(baseline).stream().filter(r -> r.getAction().equals(action)).toList();
    }

    // ------------------------------------------------------------------ requests

    public MvcResult get(String jwt, String url) throws Exception {
        return send(jwt, MockMvcRequestBuilders.get(url), null);
    }

    public MvcResult post(String jwt, String url, String jsonBody) throws Exception {
        return send(jwt, MockMvcRequestBuilders.post(url), jsonBody);
    }

    public MvcResult put(String jwt, String url, String jsonBody) throws Exception {
        return send(jwt, MockMvcRequestBuilders.put(url), jsonBody);
    }

    public MvcResult patch(String jwt, String url) throws Exception {
        return send(jwt, MockMvcRequestBuilders.patch(url), null);
    }

    public MvcResult delete(String jwt, String url) throws Exception {
        return send(jwt, MockMvcRequestBuilders.delete(url), null);
    }

    public MvcResult request(HttpMethod method, String jwt, String url) throws Exception {
        return send(jwt, MockMvcRequestBuilders.request(method, url), null);
    }

    private MvcResult send(String jwt, MockHttpServletRequestBuilder builder, String jsonBody) throws Exception {
        if (jwt != null) {
            builder.header("Authorization", "Bearer " + jwt);
        }
        if (jsonBody != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(jsonBody);
        }
        return mockMvc.perform(builder).andReturn();
    }
}
