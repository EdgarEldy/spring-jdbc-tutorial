package com.edgareldy.springjdbctutorial.ws.integration;

import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertError;
import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import com.edgareldy.springjdbctutorial.ws.config.WebMvcConfig;
import com.edgareldy.springjdbctutorial.ws.support.RbacTestSupport;
import com.edgareldy.springjdbctutorial.ws.support.RbacTestSupport.AuditRow;
import com.edgareldy.springjdbctutorial.ws.support.RbacTestSupport.TestUser;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * The last-admin rule through the REAL stack: refused in both directions (removing ROLE:WRITE from a role, removing
 * a role from a user), allowed while another enabled and unlocked holder exists, not fooled by disabled or locked
 * holders, not applied to unrelated permissions, and race free (two admins removing each other at the same time).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// The rule counts every enabled, unlocked ROLE:WRITE holder of the shared database. setUp() disables the
// holders left by other tests (RbacTestSupport.isolateLastAdminState) and tearDown() restores them, so each
// test starts with ZERO holders and creates exactly the admins it needs, on its own it-... roles: the
// seeded ADMIN role is never modified.
@SpringJUnitWebConfig(WebMvcConfig.class)
class LastAdminRuleIntegrationTest {

    private static final String[] ADMIN_PERMISSIONS = {"ROLE:WRITE", "ROLE:READ", "USER:WRITE", "USER:READ",
            "PERMISSION:READ", "PERMISSION:WRITE"};

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RbacTestSupport support;

    @BeforeEach
    void setUp() {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        support = new RbacTestSupport(mockMvc, jdbcTemplate);
        support.isolateLastAdminState();
    }

    @AfterEach
    void tearDown() {
        support.close();
    }

    private long count(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, Long.class, args);
    }

    private String removePermissionUrl(long roleId, String code) {
        return "/api/v1/roles/" + roleId + "/permissions/" + support.permissionId(code);
    }

    private static String removeRoleUrl(TestUser user, long roleId) {
        return "/api/v1/users/" + user.getId() + "/roles/" + roleId;
    }

    // ---------------------------------------------------------------- removing ROLE:WRITE from a role

    @Test
    void _01_ShouldReturn422AndChangeNothing_WhenRoleWriteIsRemovedFromTheRoleOfTheOnlyAdmin() throws Exception {
        long roleId = support.createRole(ADMIN_PERMISSIONS);
        TestUser admin = support.createUser(true, false, roleId);
        long base = support.auditBaseline();

        JsonNode json = assertError(support.delete(admin.getJwt(), removePermissionUrl(roleId, "ROLE:WRITE")), 422);

        assertThat(json.get("message").asText()).isEqualTo("Cannot remove the last account holding ROLE:WRITE");
        assertThat(count("SELECT COUNT(*) FROM role_permission WHERE role_id = ? AND permission_id = ?",
                roleId, support.permissionId("ROLE:WRITE"))).isOne();
        assertThat(support.countLastAdminCandidates()).isOne();
        List<AuditRow> rejected = support.auditSince(base, "REJECTED_REMOVE_PERMISSION_FROM_ROLE");
        assertThat(rejected).hasSize(1);
        assertThat(rejected.get(0).getEntityId()).isEqualTo(roleId);
        assertThat(rejected.get(0).getActorUserId()).isEqualTo(admin.getId());
        assertThat(support.auditSince(base, "REMOVE_PERMISSION_FROM_ROLE")).isEmpty();
    }

    @Test
    void _02_ShouldAllowIt_WhenAnotherAdminHoldsRoleWriteThroughAnotherRole() throws Exception {
        long roleA = support.createRole(ADMIN_PERMISSIONS);
        long roleB = support.createRole(ADMIN_PERMISSIONS);
        TestUser adminA = support.createUser(true, false, roleA);
        TestUser adminB = support.createUser(true, false, roleB);
        long base = support.auditBaseline();

        assertSuccess(support.delete(adminA.getJwt(), removePermissionUrl(roleA, "ROLE:WRITE")), 200);

        assertThat(support.countLastAdminCandidates()).isOne();
        assertThat(support.auditSince(base, "REMOVE_PERMISSION_FROM_ROLE")).hasSize(1);
        // Now adminB is the last holder: the same operation on its role is refused
        assertError(support.delete(adminB.getJwt(), removePermissionUrl(roleB, "ROLE:WRITE")), 422);
        assertThat(support.countLastAdminCandidates()).isOne();
    }

    @Test
    void _03_ShouldAllowIt_WhenTheRemovedPermissionIsNotRoleWrite() throws Exception {
        long roleId = support.createRole(ADMIN_PERMISSIONS);
        TestUser admin = support.createUser(true, false, roleId);

        assertSuccess(support.delete(admin.getJwt(), removePermissionUrl(roleId, "PERMISSION:READ")), 200);

        assertThat(support.countLastAdminCandidates()).isOne();
    }

    @Test
    void _04_ShouldReturn422AndRestoreThePermission_WhenAPermissionUpdateWouldStripRoleWriteFromTheOnlyAdmin()
            throws Exception {
        long roleId = support.createRole(ADMIN_PERMISSIONS);
        TestUser admin = support.createUser(true, false, roleId);
        long roleWriteId = support.permissionId("ROLE:WRITE");
        try {
            JsonNode json = assertError(support.put(admin.getJwt(), "/api/v1/permissions/" + roleWriteId,
                    "{\"resource\":\"ROLE\",\"action\":\"MANAGE\"}"), 422);

            assertThat(json.get("message").asText()).isEqualTo("Cannot remove the last account holding ROLE:WRITE");
            assertThat(count("SELECT COUNT(*) FROM permissions WHERE id = ? AND resource = 'ROLE' AND action = 'WRITE'",
                    roleWriteId)).isOne();
            assertThat(support.countLastAdminCandidates()).isOne();
        } finally {
            // Safety net: if the rule ever failed, never leave the seeded permission renamed for the other tests
            jdbcTemplate.update("UPDATE permissions SET resource = 'ROLE', action = 'WRITE' WHERE id = ?", roleWriteId);
        }
    }

    // ---------------------------------------------------------------- removing the role from a user

    @Test
    void _05_ShouldReturn422AndChangeNothing_WhenTheRoleIsRemovedFromTheOnlyAdmin() throws Exception {
        long roleId = support.createRole(ADMIN_PERMISSIONS);
        TestUser admin = support.createUser(true, false, roleId);
        long base = support.auditBaseline();

        JsonNode json = assertError(support.delete(admin.getJwt(), removeRoleUrl(admin, roleId)), 422);

        assertThat(json.get("message").asText()).isEqualTo("Cannot remove the last account holding ROLE:WRITE");
        assertThat(count("SELECT COUNT(*) FROM role_user WHERE role_id = ? AND user_id = ?", roleId, admin.getId())).isOne();
        assertThat(support.countLastAdminCandidates()).isOne();
        List<AuditRow> rejected = support.auditSince(base, "REJECTED_REMOVE_ROLE_FROM_USER");
        assertThat(rejected).hasSize(1);
        assertThat(rejected.get(0).getEntityType()).isEqualTo("USER");
        assertThat(rejected.get(0).getEntityId()).isEqualTo(admin.getId());
        assertThat(support.auditSince(base, "REMOVE_ROLE_FROM_USER")).isEmpty();
    }

    @Test
    void _06_ShouldAllowItThenRefuseTheLast_WhenASecondEnabledUnlockedAdminExists() throws Exception {
        long roleId = support.createRole(ADMIN_PERMISSIONS);
        TestUser adminA = support.createUser(true, false, roleId);
        TestUser adminB = support.createUser(true, false, roleId);
        long base = support.auditBaseline();

        assertSuccess(support.delete(adminA.getJwt(), removeRoleUrl(adminB, roleId)), 200);

        assertThat(support.countLastAdminCandidates()).isOne();
        List<AuditRow> done = support.auditSince(base, "REMOVE_ROLE_FROM_USER");
        assertThat(done).hasSize(1);
        assertThat(done.get(0).getEntityId()).isEqualTo(adminB.getId());
        assertThat(done.get(0).getActorUserId()).isEqualTo(adminA.getId());
        // adminA is now alone
        assertError(support.delete(adminA.getJwt(), removeRoleUrl(adminA, roleId)), 422);
        assertThat(support.countLastAdminCandidates()).isOne();
    }

    @Test
    void _07_ShouldStillRefuse_WhenTheOtherRoleWriteHolderIsDisabled() throws Exception {
        long roleId = support.createRole(ADMIN_PERMISSIONS);
        TestUser enabledAdmin = support.createUser(true, false, roleId);
        support.createUser(false, false, roleId);

        assertError(support.delete(enabledAdmin.getJwt(), removeRoleUrl(enabledAdmin, roleId)), 422);

        assertThat(count("SELECT COUNT(*) FROM role_user WHERE user_id = ?", enabledAdmin.getId())).isOne();
    }

    @Test
    void _08_ShouldStillRefuse_WhenTheOtherRoleWriteHolderIsLocked() throws Exception {
        long roleId = support.createRole(ADMIN_PERMISSIONS);
        TestUser enabledAdmin = support.createUser(true, false, roleId);
        support.createUser(true, true, roleId);

        assertError(support.delete(enabledAdmin.getJwt(), removeRoleUrl(enabledAdmin, roleId)), 422);

        assertThat(count("SELECT COUNT(*) FROM role_user WHERE user_id = ?", enabledAdmin.getId())).isOne();
    }

    @Test
    void _09_ShouldAllowRemovingARoleWithoutRoleWrite_WhenTheAdminKeepsItsAdminRole() throws Exception {
        long adminRole = support.createRole(ADMIN_PERMISSIONS);
        long readerRole = support.createRole("USER:READ");
        TestUser admin = support.createUser(true, false, adminRole);
        support.assignRoleInDb(admin.getId(), readerRole);

        assertSuccess(support.delete(admin.getJwt(), removeRoleUrl(admin, readerRole)), 200);

        assertThat(support.countLastAdminCandidates()).isOne();
    }

    // ---------------------------------------------------------------- concurrency

    // Two admins are the only ROLE:WRITE holders and each removes the role of one holder at the same instant.
    // Without the advisory lock both transactions would count "2 holders before", each would see itself leave
    // one behind, and both would commit: zero admins. With the lock the second one waits, then counts 1 before
    // and 0 after, and is refused. Repeated three times to give a race every chance to show.
    @Test
    void _10_ShouldLetExactlyOneSucceed_WhenTwoAdminsRemoveEachOthersRoleAtTheSameTime() throws Exception {
        long roleId = support.createRole(ADMIN_PERMISSIONS);
        TestUser adminA = support.createUser(true, false, roleId);
        TestUser adminB = support.createUser(true, false, roleId);
        long base = support.auditBaseline();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 1; round <= 3; round++) {
                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);
                Future<Integer> removeA = pool.submit(removal(adminA, adminA, roleId, ready, start));
                Future<Integer> removeB = pool.submit(removal(adminB, adminB, roleId, ready, start));
                assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
                start.countDown();
                int statusA = removeA.get(60, TimeUnit.SECONDS);
                int statusB = removeB.get(60, TimeUnit.SECONDS);

                assertThat(List.of(statusA, statusB)).as("round " + round).containsExactlyInAnyOrder(200, 422);
                assertThat(support.countLastAdminCandidates()).as("round " + round).isOne();
                assertThat(support.auditSince(base, "REMOVE_ROLE_FROM_USER")).hasSize(round);
                assertThat(support.auditSince(base, "REJECTED_REMOVE_ROLE_FROM_USER")).hasSize(round);
                // Give the role back to the one who lost it, so the next round starts with two holders again
                support.assignRoleInDb((statusA == 200 ? adminA : adminB).getId(), roleId);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<Integer> removal(TestUser caller, TestUser target, long roleId, CountDownLatch ready,
                                      CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            return support.delete(caller.getJwt(), removeRoleUrl(target, roleId)).getResponse().getStatus();
        };
    }
}
