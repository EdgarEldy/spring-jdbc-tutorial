package com.edgareldy.springjdbctutorial.ws.mvc;

import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertError;
import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.edgareldy.springjdbctutorial.core.auth.dto.PermissionDto;
import com.edgareldy.springjdbctutorial.core.auth.dto.RoleDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

/**
 * MockMvc tests of RoleController with RbacService mocked but the real security and validation: statuses,
 * ApiResponse shape, body validation per field, and the 422/404 mapping of the service refusals.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class RoleControllerTest extends AbstractRbacMvcTest {

    private static RoleDto role(long id, String name) {
        return new RoleDto(id, name, List.of(new PermissionDto(7L, "ROLE", "WRITE")));
    }

    // ---------------------------------------------------------------- GET /roles

    @Test
    void _01_ShouldReturn200WithRolesAndTheirPermissions_WhenTokenHoldsRoleRead() throws Exception {
        when(rbacService.listRoles()).thenReturn(List.of(role(1L, "ADMIN")));

        JsonNode json = assertSuccess(call(getWith("/api/v1/roles", token("ROLE:READ"))), 200);

        JsonNode first = json.get("data").get(0);
        assertThat(first.get("id").asLong()).isEqualTo(1L);
        assertThat(first.get("roleName").asText()).isEqualTo("ADMIN");
        assertThat(first.get("permissions").get(0).get("resource").asText()).isEqualTo("ROLE");
        assertThat(first.get("permissions").get(0).get("action").asText()).isEqualTo("WRITE");
    }

    @Test
    void _02_ShouldReturn200WithEmptyArray_WhenThereIsNoRole() throws Exception {
        when(rbacService.listRoles()).thenReturn(List.of());

        JsonNode json = assertSuccess(call(getWith("/api/v1/roles", token("ROLE:READ"))), 200);

        assertThat(json.get("data").isArray()).isTrue();
        assertThat(json.get("data")).isEmpty();
    }

    // ---------------------------------------------------------------- POST /roles

    @Test
    void _03_ShouldReturn201WithCreatedRole_WhenBodyIsValid() throws Exception {
        when(rbacService.createRole(any(RoleDto.class))).thenReturn(role(9L, "Reviewer"));

        JsonNode json = assertSuccess(call(postJson("/api/v1/roles", token("ROLE:WRITE"), "{\"roleName\":\"Reviewer\"}")), 201);

        assertThat(json.get("data").get("id").asLong()).isEqualTo(9L);
        assertThat(json.get("message").asText()).isEqualTo("Role created");
        ArgumentCaptor<RoleDto> sent = ArgumentCaptor.forClass(RoleDto.class);
        verify(rbacService).createRole(sent.capture());
        assertThat(sent.getValue().getRoleName()).isEqualTo("Reviewer");
    }

    @Test
    void _04_ShouldReturn400NamingTheField_WhenRoleNameIsBlankOrMissing() throws Exception {
        JsonNode blank = assertError(call(postJson("/api/v1/roles", token("ROLE:WRITE"), "{\"roleName\":\"  \"}")), 400);
        JsonNode missing = assertError(call(postJson("/api/v1/roles", token("ROLE:WRITE"), "{}")), 400);

        assertThat(blank.get("message").asText()).contains("roleName: ");
        assertThat(missing.get("message").asText()).contains("roleName: ");
        verifyNoInteractions(rbacService);
    }

    @Test
    void _05_ShouldReturn400NamingTheField_WhenRoleNameExceeds100Characters() throws Exception {
        JsonNode json = assertError(call(postJson("/api/v1/roles", token("ROLE:WRITE"),
                "{\"roleName\":\"" + "x".repeat(101) + "\"}")), 400);

        assertThat(json.get("message").asText()).contains("roleName: ");
        verifyNoInteractions(rbacService);
    }

    @Test
    void _06_ShouldReturn400_WhenRoleJsonIsMalformed() throws Exception {
        assertError(call(postJson("/api/v1/roles", token("ROLE:WRITE"), "{\"roleName\":")), 400);
    }

    @Test
    void _07_ShouldReturn415_WhenRoleContentTypeIsNotJson() throws Exception {
        assertError(call(post("/api/v1/roles").header("Authorization", bearer(token("ROLE:WRITE")))
                .contentType(MediaType.TEXT_PLAIN).content("Reviewer")), 415);
    }

    @Test
    void _08_ShouldReturn422_WhenRoleNameAlreadyExists() throws Exception {
        when(rbacService.createRole(any(RoleDto.class))).thenThrow(new BusinessRuleException("Role name already exists"));

        JsonNode json = assertError(call(postJson("/api/v1/roles", token("ROLE:WRITE"), "{\"roleName\":\"ADMIN\"}")), 422);

        assertThat(json.get("message").asText()).isEqualTo("Role name already exists");
    }

    // ---------------------------------------------------------------- PUT /roles/{id}

    @Test
    void _09_ShouldReturn200WithRenamedRole_WhenBodyIsValid() throws Exception {
        when(rbacService.updateRole(anyLong(), any(RoleDto.class))).thenReturn(role(3L, "Editors"));

        JsonNode json = assertSuccess(call(putJson("/api/v1/roles/3", token("ROLE:WRITE"), "{\"roleName\":\"Editors\"}")), 200);

        assertThat(json.get("data").get("roleName").asText()).isEqualTo("Editors");
        ArgumentCaptor<RoleDto> sent = ArgumentCaptor.forClass(RoleDto.class);
        verify(rbacService).updateRole(eq(3L), sent.capture());
        assertThat(sent.getValue().getRoleName()).isEqualTo("Editors");
    }

    @Test
    void _10_ShouldReturn400NamingTheField_WhenRenameBodyIsInvalid() throws Exception {
        JsonNode json = assertError(call(putJson("/api/v1/roles/3", token("ROLE:WRITE"), "{\"roleName\":\"\"}")), 400);

        assertThat(json.get("message").asText()).contains("roleName: ");
        verifyNoInteractions(rbacService);
    }

    @Test
    void _11_ShouldReturn404_WhenRoleToRenameDoesNotExist() throws Exception {
        when(rbacService.updateRole(anyLong(), any(RoleDto.class))).thenThrow(new ResourceNotFoundException("Role not found: 99"));

        assertError(call(putJson("/api/v1/roles/99", token("ROLE:WRITE"), "{\"roleName\":\"X\"}")), 404);
    }

    @Test
    void _12_ShouldReturn422_WhenRenameCollidesWithAnotherRole() throws Exception {
        when(rbacService.updateRole(anyLong(), any(RoleDto.class))).thenThrow(new BusinessRuleException("Role name already exists"));

        assertError(call(putJson("/api/v1/roles/3", token("ROLE:WRITE"), "{\"roleName\":\"ADMIN\"}")), 422);
    }

    // ---------------------------------------------------------------- DELETE /roles/{id}

    @Test
    void _13_ShouldReturn200WithNoData_WhenRoleIsDeleted() throws Exception {
        JsonNode json = assertSuccess(call(deleteWith("/api/v1/roles/3", token("ROLE:WRITE"))), 200);

        assertThat(json.get("message").asText()).isEqualTo("Role deleted");
        assertThat(json.get("data") == null || json.get("data").isNull()).isTrue();
        verify(rbacService).deleteRole(3L);
    }

    @Test
    void _14_ShouldReturn422_WhenRoleIsStillAssignedToUsers() throws Exception {
        doThrow(new BusinessRuleException("Role is still assigned to users")).when(rbacService).deleteRole(3L);

        JsonNode json = assertError(call(deleteWith("/api/v1/roles/3", token("ROLE:WRITE"))), 422);

        assertThat(json.get("message").asText()).isEqualTo("Role is still assigned to users");
    }

    @Test
    void _15_ShouldReturn404_WhenRoleToDeleteDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Role not found: 99")).when(rbacService).deleteRole(99L);

        assertError(call(deleteWith("/api/v1/roles/99", token("ROLE:WRITE"))), 404);
    }

    // ---------------------------------------------------------------- POST /roles/{id}/permissions/{permissionId}

    @Test
    void _16_ShouldReturn200WithRole_WhenPermissionIsAssigned() throws Exception {
        when(rbacService.assignPermissionToRole(3L, 7L)).thenReturn(role(3L, "EDITOR"));

        JsonNode json = assertSuccess(call(postWith("/api/v1/roles/3/permissions/7", token("ROLE:WRITE"))), 200);

        assertThat(json.get("message").asText()).isEqualTo("Permission assigned");
        assertThat(json.get("data").get("permissions")).hasSize(1);
    }

    @Test
    void _17_ShouldReturn422_WhenPermissionIsAlreadyAssigned() throws Exception {
        when(rbacService.assignPermissionToRole(3L, 7L))
                .thenThrow(new BusinessRuleException("Permission is already assigned to this role"));

        assertError(call(postWith("/api/v1/roles/3/permissions/7", token("ROLE:WRITE"))), 422);
    }

    @Test
    void _18_ShouldReturn404_WhenRoleOrPermissionToAssignDoesNotExist() throws Exception {
        when(rbacService.assignPermissionToRole(3L, 99L)).thenThrow(new ResourceNotFoundException("Permission not found: 99"));

        assertError(call(postWith("/api/v1/roles/3/permissions/99", token("ROLE:WRITE"))), 404);
    }

    // ---------------------------------------------------------------- DELETE /roles/{id}/permissions/{permissionId}

    @Test
    void _19_ShouldReturn200WithRole_WhenPermissionIsRemoved() throws Exception {
        when(rbacService.removePermissionFromRole(3L, 7L)).thenReturn(role(3L, "EDITOR"));

        JsonNode json = assertSuccess(call(deleteWith("/api/v1/roles/3/permissions/7", token("ROLE:WRITE"))), 200);

        assertThat(json.get("message").asText()).isEqualTo("Permission removed");
    }

    @Test
    void _20_ShouldReturn422_WhenRemovalWouldLeaveNoAdmin() throws Exception {
        when(rbacService.removePermissionFromRole(3L, 7L))
                .thenThrow(new BusinessRuleException("Cannot remove the last account holding ROLE:WRITE"));

        JsonNode json = assertError(call(deleteWith("/api/v1/roles/3/permissions/7", token("ROLE:WRITE"))), 422);

        assertThat(json.get("message").asText()).contains("last account");
    }

    @Test
    void _21_ShouldReturn404_WhenPermissionIsNotAssignedToTheRole() throws Exception {
        when(rbacService.removePermissionFromRole(3L, 7L))
                .thenThrow(new ResourceNotFoundException("Permission 7 is not assigned to role 3"));

        assertError(call(deleteWith("/api/v1/roles/3/permissions/7", token("ROLE:WRITE"))), 404);
    }

    @Test
    void _22_ShouldReturn405ApiResponse_WhenPatchIsUsedOnRoles() throws Exception {
        assertError(call(patchWith("/api/v1/roles/3", token("ROLE:WRITE"))), 405);
    }
}
