package com.edgareldy.springjdbctutorial.ws.integration;

import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertError;
import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import com.edgareldy.springjdbctutorial.ws.config.WebMvcConfig;
import com.edgareldy.springjdbctutorial.ws.support.RbacTestSupport;
import com.edgareldy.springjdbctutorial.ws.support.RbacTestSupport.AuditRow;
import com.edgareldy.springjdbctutorial.ws.support.RbacTestSupport.TestUser;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * End-to-end RBAC flows through the REAL stack (real WebMvcConfig, real services, real PostgreSQL migrated by
 * Flyway): permission enforcement, role/permission/user-role CRUD, JWT permissions applied at the next login,
 * refused operations with their REJECTED_ audit row (written through the transactional proxy), audit rows for
 * every mutation, 404s that are not audited, and page/size validation.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// The full application context: DataSourceConfig (Flyway + Hikari), the auth services, SecurityConfig and the
// controllers. Nothing is mocked. All test classes of the JVM share the one Testcontainers database, so each
// test creates its own uniquely named users/roles and RbacTestSupport removes them afterwards.
@SpringJUnitWebConfig(WebMvcConfig.class)
class RbacFlowIntegrationTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private RbacTestSupport support;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        support = new RbacTestSupport(mockMvc, jdbcTemplate);
        // Other tests may have left ROLE:WRITE holders behind: disable them so the last-admin rule only sees this test
        support.isolateLastAdminState();
    }

    @AfterEach
    void tearDown() {
        support.close();
    }

    private static String roleBody(String name) {
        return "{\"roleName\":\"" + name + "\"}";
    }

    private static String permissionBody(String resource, String action) {
        return "{\"resource\":\"" + resource + "\",\"action\":\"" + action + "\"}";
    }

    private static long idOf(JsonNode json) {
        return json.get("data").get("id").asLong();
    }

    private static List<String> actions(List<AuditRow> rows) {
        return rows.stream().map(AuditRow::getAction).toList();
    }

    private boolean listContainsRole(String jwt, String roleName) throws Exception {
        JsonNode json = assertSuccess(support.get(jwt, "/api/v1/roles"), 200);
        for (JsonNode role : json.get("data")) {
            if (role.get("roleName").asText().equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    private long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    // ---------------------------------------------------------------- permission enforcement

    @Test
    void _01_ShouldEnforcePermissionsEndToEnd_WhenSimpleUserAndAdminCallTheListRoutes() throws Exception {
        TestUser simple = support.createSimpleUser();
        TestUser admin = support.createAdmin();

        for (String url : List.of("/api/v1/users", "/api/v1/roles", "/api/v1/permissions")) {
            assertError(support.get(null, url), 401);
            assertError(support.get(simple.getJwt(), url), 403);
            assertSuccess(support.get(admin.getJwt(), url), 200);
        }
        // A user without permission is still authenticated: routes that need no permission stay open
        assertSuccess(support.get(simple.getJwt(), "/api/v1/auth/me"), 200);
    }

    @Test
    void _02_ShouldRefuseEveryMutation_WhenSimpleUserHasNoPermission() throws Exception {
        TestUser simple = support.createSimpleUser();
        long base = support.auditBaseline();

        assertError(support.post(simple.getJwt(), "/api/v1/roles", roleBody(RbacTestSupport.uniqueRoleName())), 403);
        assertError(support.post(simple.getJwt(), "/api/v1/permissions", permissionBody("IT_NOPE", "READ")), 403);
        assertError(support.patch(simple.getJwt(), "/api/v1/users/" + simple.getId() + "/roles/" + support.adminRoleId()), 403);

        // The escalation attempt changed nothing and left no audit row (the service was never reached)
        assertThat(count("SELECT COUNT(*) FROM role_user WHERE user_id = ?", simple.getId())).isZero();
        assertThat(support.auditSince(base)).isEmpty();
    }

    @Test
    void _03_ShouldExposeTheSeededAdminRoleWithFourteenPermissions_WhenAdminListsRolesAndPermissions() throws Exception {
        TestUser admin = support.createAdmin();

        JsonNode roles = assertSuccess(support.get(admin.getJwt(), "/api/v1/roles"), 200).get("data");
        JsonNode adminRole = null;
        for (JsonNode role : roles) {
            if (role.get("roleName").asText().equals("ADMIN")) {
                adminRole = role;
            }
        }
        JsonNode permissions = assertSuccess(support.get(admin.getJwt(), "/api/v1/permissions"), 200).get("data");

        assertThat(adminRole).isNotNull();
        assertThat(adminRole.get("permissions")).hasSize(14);
        assertThat(permissions.size()).isGreaterThanOrEqualTo(14);
    }

    // ---------------------------------------------------------------- CRUD flows and audit rows

    @Test
    void _04_ShouldCreateRenameAndDeleteARoleWithAnAuditRowEach_WhenTwoAdminsCollaborate() throws Exception {
        TestUser adminA = support.createAdmin();
        TestUser adminB = support.createAdmin();
        long base = support.auditBaseline();
        String name = RbacTestSupport.uniqueRoleName();
        String renamed = RbacTestSupport.uniqueRoleName();

        JsonNode created = assertSuccess(support.post(adminA.getJwt(), "/api/v1/roles", roleBody(name)), 201);
        long roleId = idOf(created);
        assertThat(created.get("data").get("permissions")).isEmpty();
        JsonNode updated = assertSuccess(support.put(adminB.getJwt(), "/api/v1/roles/" + roleId, roleBody(renamed)), 200);
        assertThat(updated.get("data").get("roleName").asText()).isEqualTo(renamed);
        assertThat(listContainsRole(adminA.getJwt(), renamed)).isTrue();
        assertSuccess(support.delete(adminA.getJwt(), "/api/v1/roles/" + roleId), 200);
        assertThat(listContainsRole(adminA.getJwt(), renamed)).isFalse();

        List<AuditRow> rows = support.auditSince(base);
        assertThat(actions(rows)).containsExactly("CREATE_ROLE", "UPDATE_ROLE", "DELETE_ROLE");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getEntityType()).isEqualTo("ROLE");
            assertThat(row.getEntityId()).isEqualTo(roleId);
        });
        // The actor of each row is the caller of that very request
        assertThat(rows.get(0).getActorUserId()).isEqualTo(adminA.getId());
        assertThat(rows.get(1).getActorUserId()).isEqualTo(adminB.getId());
        assertThat(rows.get(2).getActorUserId()).isEqualTo(adminA.getId());
        assertThat(rows.get(1).getDetails()).contains(name).contains(renamed);
    }

    @Test
    void _05_ShouldCreateNormalizeUpdateAndDeleteAPermissionWithAnAuditRowEach_WhenAdminManagesPermissions()
            throws Exception {
        TestUser admin = support.createAdmin();
        long base = support.auditBaseline();

        JsonNode created = assertSuccess(support.post(admin.getJwt(), "/api/v1/permissions",
                permissionBody("it_invoice", "read")), 201);
        long id = idOf(created);
        assertThat(created.get("data").get("resource").asText()).isEqualTo("IT_INVOICE");
        assertThat(created.get("data").get("action").asText()).isEqualTo("READ");
        JsonNode updated = assertSuccess(support.put(admin.getJwt(), "/api/v1/permissions/" + id,
                permissionBody("it_invoice", "write")), 200);
        assertThat(updated.get("data").get("action").asText()).isEqualTo("WRITE");
        assertSuccess(support.delete(admin.getJwt(), "/api/v1/permissions/" + id), 200);

        List<AuditRow> rows = support.auditSince(base);
        assertThat(actions(rows)).containsExactly("CREATE_PERMISSION", "UPDATE_PERMISSION", "DELETE_PERMISSION");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getEntityType()).isEqualTo("PERMISSION");
            assertThat(row.getEntityId()).isEqualTo(id);
            assertThat(row.getActorUserId()).isEqualTo(admin.getId());
        });
        assertThat(count("SELECT COUNT(*) FROM permissions WHERE id = ?", id)).isZero();
    }

    @Test
    void _06_ShouldAssignAndRemoveInTheRightDirections_WhenPermissionsGoOntoARoleAndTheRoleOntoAUser()
            throws Exception {
        TestUser admin = support.createAdmin();
        TestUser target = support.createSimpleUser();
        long base = support.auditBaseline();
        long roleId = idOf(assertSuccess(support.post(admin.getJwt(), "/api/v1/roles",
                roleBody(RbacTestSupport.uniqueRoleName())), 201));
        long permissionId = idOf(assertSuccess(support.post(admin.getJwt(), "/api/v1/permissions",
                permissionBody("it_report", "read")), 201));

        JsonNode withPermission = assertSuccess(support.post(admin.getJwt(),
                "/api/v1/roles/" + roleId + "/permissions/" + permissionId, null), 200);
        assertThat(withPermission.get("data").get("permissions")).hasSize(1);
        JsonNode withRole = assertSuccess(support.patch(admin.getJwt(),
                "/api/v1/users/" + target.getId() + "/roles/" + roleId), 200);
        assertThat(withRole.get("data").get("roles")).hasSize(1);
        assertThat(withRole.get("data").get("permissions").get(0).asText()).isEqualTo("IT_REPORT:READ");
        JsonNode detail = assertSuccess(support.get(admin.getJwt(), "/api/v1/users/" + target.getId()), 200);
        assertThat(detail.get("data").get("permissions").get(0).asText()).isEqualTo("IT_REPORT:READ");
        assertThat(detail.get("data").has("password")).isFalse();

        JsonNode withoutRole = assertSuccess(support.delete(admin.getJwt(),
                "/api/v1/users/" + target.getId() + "/roles/" + roleId), 200);
        assertThat(withoutRole.get("data").get("roles")).isEmpty();
        JsonNode withoutPermission = assertSuccess(support.delete(admin.getJwt(),
                "/api/v1/roles/" + roleId + "/permissions/" + permissionId), 200);
        assertThat(withoutPermission.get("data").get("permissions")).isEmpty();

        List<AuditRow> rows = support.auditSince(base);
        assertThat(actions(rows)).containsExactly("CREATE_ROLE", "CREATE_PERMISSION", "ASSIGN_PERMISSION_TO_ROLE",
                "ASSIGN_ROLE_TO_USER", "REMOVE_ROLE_FROM_USER", "REMOVE_PERMISSION_FROM_ROLE");
        assertThat(rows.get(2).getEntityType()).isEqualTo("ROLE");
        assertThat(rows.get(2).getEntityId()).isEqualTo(roleId);
        assertThat(rows.get(3).getEntityType()).isEqualTo("USER");
        assertThat(rows.get(3).getEntityId()).isEqualTo(target.getId());
        assertThat(rows.get(4).getEntityType()).isEqualTo("USER");
        assertThat(rows.get(4).getEntityId()).isEqualTo(target.getId());
        assertThat(rows.get(5).getEntityType()).isEqualTo("ROLE");
        assertThat(rows).allSatisfy(row -> assertThat(row.getActorUserId()).isEqualTo(admin.getId()));
    }

    // ---------------------------------------------------------------- JWT permissions apply at the next login

    @Test
    void _07_ShouldApplyRoleChangesOnlyAtTheNextLogin_WhenTheRoleIsAssignedAndRemovedAfterLogin() throws Exception {
        TestUser admin = support.createAdmin();
        TestUser user = support.createSimpleUser();
        long roleId = support.createRole("ROLE:READ");
        String tokenBefore = user.getJwt();
        assertError(support.get(tokenBefore, "/api/v1/roles"), 403);

        assertSuccess(support.patch(admin.getJwt(), "/api/v1/users/" + user.getId() + "/roles/" + roleId), 200);

        // Permissions are embedded in the JWT: the old token still lacks ROLE:READ ...
        assertError(support.get(tokenBefore, "/api/v1/roles"), 403);
        // ... and a NEW login carries it
        String tokenAfter = support.freshToken(user);
        assertSuccess(support.get(tokenAfter, "/api/v1/roles"), 200);

        assertSuccess(support.delete(admin.getJwt(), "/api/v1/users/" + user.getId() + "/roles/" + roleId), 200);

        // Removal is symmetric: the token issued while the role was held keeps working until it expires ...
        assertSuccess(support.get(tokenAfter, "/api/v1/roles"), 200);
        // ... and the next login no longer has the permission
        assertError(support.get(support.freshToken(user), "/api/v1/roles"), 403);
    }

    // ---------------------------------------------------------------- refusals and their audit rows

    @Test
    void _08_ShouldReturn422AndKeepTheRefusalRowAfterTheRollback_WhenDeletingARoleStillAssigned() throws Exception {
        TestUser admin = support.createAdmin();
        long roleId = support.createRole("USER:READ");
        TestUser holder = support.createUser(true, false, roleId);
        long base = support.auditBaseline();

        JsonNode json = assertError(support.delete(admin.getJwt(), "/api/v1/roles/" + roleId), 422);

        assertThat(json.get("message").asText()).isEqualTo("Role is still assigned to users");
        assertThat(count("SELECT COUNT(*) FROM roles WHERE id = ?", roleId)).isOne();
        assertThat(count("SELECT COUNT(*) FROM role_user WHERE role_id = ? AND user_id = ?", roleId, holder.getId())).isOne();
        // The business transaction rolled back, yet the REQUIRES_NEW refusal row is committed: proof that the
        // AuditLogger is really called through its transactional proxy
        List<AuditRow> rejected = support.auditSince(base, "REJECTED_DELETE_ROLE");
        assertThat(rejected).hasSize(1);
        assertThat(rejected.get(0).getEntityType()).isEqualTo("ROLE");
        assertThat(rejected.get(0).getEntityId()).isEqualTo(roleId);
        assertThat(rejected.get(0).getActorUserId()).isEqualTo(admin.getId());
        assertThat(rejected.get(0).getDetails()).contains("reason=Role is still assigned to users");
        assertThat(support.auditSince(base, "DELETE_ROLE")).isEmpty();
    }

    @Test
    void _09_ShouldReturn422AndKeepTheRefusalRow_WhenDeletingAPermissionStillHeldByARole() throws Exception {
        TestUser admin = support.createAdmin();
        long roleId = support.createRole("USER:READ");
        long permissionId = idOf(assertSuccess(support.post(admin.getJwt(), "/api/v1/permissions",
                permissionBody("it_held", "read")), 201));
        assertSuccess(support.post(admin.getJwt(), "/api/v1/roles/" + roleId + "/permissions/" + permissionId, null), 200);
        long base = support.auditBaseline();

        JsonNode json = assertError(support.delete(admin.getJwt(), "/api/v1/permissions/" + permissionId), 422);

        assertThat(json.get("message").asText()).isEqualTo("Permission is still assigned to roles");
        assertThat(count("SELECT COUNT(*) FROM permissions WHERE id = ?", permissionId)).isOne();
        List<AuditRow> rejected = support.auditSince(base, "REJECTED_DELETE_PERMISSION");
        assertThat(rejected).hasSize(1);
        assertThat(rejected.get(0).getEntityId()).isEqualTo(permissionId);
        assertThat(rejected.get(0).getActorUserId()).isEqualTo(admin.getId());
        assertThat(support.auditSince(base, "DELETE_PERMISSION")).isEmpty();
    }

    @Test
    void _10_ShouldReturn422WithARefusalRowEach_WhenCreatingOrAssigningDuplicates() throws Exception {
        TestUser admin = support.createAdmin();
        TestUser target = support.createSimpleUser();
        String roleName = RbacTestSupport.uniqueRoleName();
        long roleId = idOf(assertSuccess(support.post(admin.getJwt(), "/api/v1/roles", roleBody(roleName)), 201));
        long permissionId = idOf(assertSuccess(support.post(admin.getJwt(), "/api/v1/permissions",
                permissionBody("it_dup", "read")), 201));
        assertSuccess(support.post(admin.getJwt(), "/api/v1/roles/" + roleId + "/permissions/" + permissionId, null), 200);
        assertSuccess(support.patch(admin.getJwt(), "/api/v1/users/" + target.getId() + "/roles/" + roleId), 200);
        long base = support.auditBaseline();

        assertError(support.post(admin.getJwt(), "/api/v1/roles", roleBody(roleName)), 422);
        // The same permission written in another case is the same permission (values are normalised)
        assertError(support.post(admin.getJwt(), "/api/v1/permissions", permissionBody("IT_DUP", "READ")), 422);
        assertError(support.post(admin.getJwt(), "/api/v1/roles/" + roleId + "/permissions/" + permissionId, null), 422);
        assertError(support.patch(admin.getJwt(), "/api/v1/users/" + target.getId() + "/roles/" + roleId), 422);

        assertThat(actions(support.auditSince(base))).containsExactly("REJECTED_CREATE_ROLE",
                "REJECTED_CREATE_PERMISSION", "REJECTED_ASSIGN_PERMISSION_TO_ROLE", "REJECTED_ASSIGN_ROLE_TO_USER");
        assertThat(support.auditSince(base)).allSatisfy(row ->
                assertThat(row.getActorUserId()).isEqualTo(admin.getId()));
        assertThat(count("SELECT COUNT(*) FROM roles WHERE role_name = ?", roleName)).isOne();
        assertThat(count("SELECT COUNT(*) FROM permissions WHERE resource = 'IT_DUP'")).isOne();
    }

    @Test
    void _11_ShouldWriteNoAuditRow_WhenTheTargetDoesNotExist() throws Exception {
        TestUser admin = support.createAdmin();
        TestUser simple = support.createSimpleUser();
        long roleId = support.createRole("USER:READ");
        long base = support.auditBaseline();
        String jwt = admin.getJwt();
        long missing = 999_999_999L;

        assertError(support.get(jwt, "/api/v1/users/" + missing), 404);
        assertError(support.patch(jwt, "/api/v1/users/" + missing + "/roles/" + roleId), 404);
        assertError(support.patch(jwt, "/api/v1/users/" + simple.getId() + "/roles/" + missing), 404);
        assertError(support.delete(jwt, "/api/v1/users/" + missing + "/roles/" + roleId), 404);
        // The role exists but is not assigned to this user
        assertError(support.delete(jwt, "/api/v1/users/" + simple.getId() + "/roles/" + roleId), 404);
        assertError(support.put(jwt, "/api/v1/roles/" + missing, roleBody("x")), 404);
        assertError(support.delete(jwt, "/api/v1/roles/" + missing), 404);
        assertError(support.post(jwt, "/api/v1/roles/" + missing + "/permissions/" + support.permissionId("USER:READ"), null), 404);
        assertError(support.post(jwt, "/api/v1/roles/" + roleId + "/permissions/" + missing, null), 404);
        // The permission exists but is not assigned to this role
        assertError(support.delete(jwt, "/api/v1/roles/" + roleId + "/permissions/" + support.permissionId("USER:WRITE")), 404);
        assertError(support.put(jwt, "/api/v1/permissions/" + missing, permissionBody("IT_X", "READ")), 404);
        assertError(support.delete(jwt, "/api/v1/permissions/" + missing), 404);

        assertThat(support.auditSince(base)).isEmpty();
    }

    @Test
    void _12_ShouldReturn400WithoutAnyAuditRow_WhenBodiesAreInvalid() throws Exception {
        TestUser admin = support.createAdmin();
        long base = support.auditBaseline();

        JsonNode role = assertError(support.post(admin.getJwt(), "/api/v1/roles", "{\"roleName\":\"\"}"), 400);
        JsonNode permission = assertError(support.post(admin.getJwt(), "/api/v1/permissions",
                permissionBody("bad-resource", "READ")), 400);

        assertThat(role.get("message").asText()).contains("roleName: ");
        assertThat(permission.get("message").asText()).contains("resource: ");
        assertThat(support.auditSince(base)).isEmpty();
    }

    // ---------------------------------------------------------------- GET /users paging and other HTTP errors

    @Test
    void _13_ShouldReturn400NamingTheParameter_WhenPageOrSizeIsOutOfBounds() throws Exception {
        TestUser admin = support.createAdmin();

        JsonNode page = assertError(support.get(admin.getJwt(), "/api/v1/users?page=-1"), 400);
        JsonNode zero = assertError(support.get(admin.getJwt(), "/api/v1/users?size=0"), 400);
        JsonNode big = assertError(support.get(admin.getJwt(), "/api/v1/users?size=101"), 400);
        JsonNode both = assertError(support.get(admin.getJwt(), "/api/v1/users?page=-1&size=0"), 400);

        assertThat(page.get("message").asText()).contains("page: ");
        assertThat(zero.get("message").asText()).contains("size: ");
        assertThat(big.get("message").asText()).contains("size: ");
        assertThat(both.get("message").asText()).contains("page: ").contains("size: ");
    }

    @Test
    void _14_ShouldKeepEmptyContentInTheJson_WhenPageIsBeyondTheLastOne() throws Exception {
        TestUser admin = support.createAdmin();

        JsonNode json = assertSuccess(support.get(admin.getJwt(), "/api/v1/users?page=100000&size=10"), 200);

        JsonNode data = json.get("data");
        assertThat(data.has("content")).isTrue();
        assertThat(data.get("content").isArray()).isTrue();
        assertThat(data.get("content")).isEmpty();
        assertThat(data.get("totalElements").asLong()).isGreaterThanOrEqualTo(1L);
        assertThat(data.get("page").asInt()).isEqualTo(100000);
    }

    @Test
    void _15_ShouldReturnTheRequestedSlice_WhenSizeIsOne() throws Exception {
        TestUser admin = support.createAdmin();
        support.createSimpleUser();

        JsonNode data = assertSuccess(support.get(admin.getJwt(), "/api/v1/users?page=0&size=1"), 200).get("data");

        assertThat(data.get("content")).hasSize(1);
        assertThat(data.get("size").asInt()).isEqualTo(1);
        assertThat(data.get("totalElements").asLong()).isGreaterThanOrEqualTo(2L);
        assertThat(data.get("totalPages").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(data.get("content").get(0).has("password")).isFalse();
    }

    @Test
    void _16_ShouldReturnRolesAndPermissionsAndNoPassword_WhenAdminReadsAnAdminUser() throws Exception {
        TestUser admin = support.createAdmin();

        JsonNode data = assertSuccess(support.get(admin.getJwt(), "/api/v1/users/" + admin.getId()), 200).get("data");

        assertThat(data.get("roles").get(0).asText()).isEqualTo("ADMIN");
        assertThat(data.get("permissions")).hasSize(14);
        assertThat(data.has("password")).isFalse();
    }

    @Test
    void _17_ShouldReturn405And415AndBadRequestAsApiResponses_WhenTheRequestIsMalformed() throws Exception {
        TestUser admin = support.createAdmin();

        assertError(support.post(admin.getJwt(), "/api/v1/users", "{}"), 405);
        MvcResult wrongType = mockMvc.perform(post("/api/v1/roles").header("Authorization", "Bearer " + admin.getJwt())
                .contentType(MediaType.TEXT_PLAIN).content("name")).andReturn();
        assertError(wrongType, 415);
        assertError(support.get(admin.getJwt(), "/api/v1/users/not-a-number"), 400);
    }
}
