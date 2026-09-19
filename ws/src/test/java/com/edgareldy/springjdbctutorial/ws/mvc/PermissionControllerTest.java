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

import com.edgareldy.springjdbctutorial.core.auth.dto.PermissionDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * MockMvc tests of PermissionController with RbacService mocked but the real security and validation: statuses,
 * ApiResponse shape, per-field body validation (letters and underscore only) and the 422/404 refusals.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class PermissionControllerTest extends AbstractRbacMvcTest {

    private static final String VALID_BODY = "{\"resource\":\"invoice\",\"action\":\"read\"}";

    private static PermissionDto permission(long id, String resource, String action) {
        return new PermissionDto(id, resource, action);
    }

    @Test
    void _01_ShouldReturn200WithPermissions_WhenTokenHoldsPermissionRead() throws Exception {
        when(rbacService.listPermissions()).thenReturn(List.of(permission(1L, "USER", "READ")));

        JsonNode json = assertSuccess(call(getWith("/api/v1/permissions", token("PERMISSION:READ"))), 200);

        assertThat(json.get("data").get(0).get("id").asLong()).isEqualTo(1L);
        assertThat(json.get("data").get(0).get("resource").asText()).isEqualTo("USER");
        assertThat(json.get("data").get(0).get("action").asText()).isEqualTo("READ");
    }

    @Test
    void _02_ShouldReturn201WithCreatedPermission_WhenBodyIsValid() throws Exception {
        when(rbacService.createPermission(any(PermissionDto.class))).thenReturn(permission(20L, "INVOICE", "READ"));

        JsonNode json = assertSuccess(call(postJson("/api/v1/permissions", token("PERMISSION:WRITE"), VALID_BODY)), 201);

        assertThat(json.get("data").get("resource").asText()).isEqualTo("INVOICE");
        assertThat(json.get("message").asText()).isEqualTo("Permission created");
        ArgumentCaptor<PermissionDto> sent = ArgumentCaptor.forClass(PermissionDto.class);
        verify(rbacService).createPermission(sent.capture());
        assertThat(sent.getValue().getResource()).isEqualTo("invoice");
        assertThat(sent.getValue().getAction()).isEqualTo("read");
    }

    @Test
    void _03_ShouldReturn400NamingBothFields_WhenResourceAndActionAreBlank() throws Exception {
        JsonNode json = assertError(call(postJson("/api/v1/permissions", token("PERMISSION:WRITE"),
                "{\"resource\":\"\",\"action\":\" \"}")), 400);

        assertThat(json.get("message").asText()).contains("resource: ").contains("action: ");
        verifyNoInteractions(rbacService);
    }

    @Test
    void _04_ShouldReturn400NamingOnlyTheBadField_WhenResourceHasForbiddenCharacters() throws Exception {
        JsonNode json = assertError(call(postJson("/api/v1/permissions", token("PERMISSION:WRITE"),
                "{\"resource\":\"INVOICE-1\",\"action\":\"READ\"}")), 400);

        assertThat(json.get("message").asText()).contains("resource: ").doesNotContain("action: ");
        verifyNoInteractions(rbacService);
    }

    @Test
    void _05_ShouldReturn400NamingTheField_WhenActionExceeds100Characters() throws Exception {
        JsonNode json = assertError(call(postJson("/api/v1/permissions", token("PERMISSION:WRITE"),
                "{\"resource\":\"INVOICE\",\"action\":\"" + "A".repeat(101) + "\"}")), 400);

        assertThat(json.get("message").asText()).contains("action: ");
    }

    @Test
    void _06_ShouldReturn400_WhenPermissionJsonIsMalformed() throws Exception {
        assertError(call(postJson("/api/v1/permissions", token("PERMISSION:WRITE"), "not json")), 400);
    }

    @Test
    void _07_ShouldReturn422_WhenPermissionAlreadyExists() throws Exception {
        when(rbacService.createPermission(any(PermissionDto.class)))
                .thenThrow(new BusinessRuleException("Permission already exists"));

        JsonNode json = assertError(call(postJson("/api/v1/permissions", token("PERMISSION:WRITE"), VALID_BODY)), 422);

        assertThat(json.get("message").asText()).isEqualTo("Permission already exists");
    }

    @Test
    void _08_ShouldReturn200WithUpdatedPermission_WhenBodyIsValid() throws Exception {
        when(rbacService.updatePermission(anyLong(), any(PermissionDto.class)))
                .thenReturn(permission(7L, "INVOICE", "READ"));

        JsonNode json = assertSuccess(call(putJson("/api/v1/permissions/7", token("PERMISSION:WRITE"), VALID_BODY)), 200);

        assertThat(json.get("data").get("id").asLong()).isEqualTo(7L);
        verify(rbacService).updatePermission(eq(7L), any(PermissionDto.class));
    }

    @Test
    void _09_ShouldReturn400NamingTheFields_WhenUpdateBodyIsInvalid() throws Exception {
        JsonNode json = assertError(call(putJson("/api/v1/permissions/7", token("PERMISSION:WRITE"), "{}")), 400);

        assertThat(json.get("message").asText()).contains("resource: ").contains("action: ");
        verifyNoInteractions(rbacService);
    }

    @Test
    void _10_ShouldReturn404_WhenPermissionToUpdateDoesNotExist() throws Exception {
        when(rbacService.updatePermission(anyLong(), any(PermissionDto.class)))
                .thenThrow(new ResourceNotFoundException("Permission not found: 99"));

        assertError(call(putJson("/api/v1/permissions/99", token("PERMISSION:WRITE"), VALID_BODY)), 404);
    }

    @Test
    void _11_ShouldReturn422_WhenUpdateWouldStripRoleWriteFromTheLastAdmin() throws Exception {
        when(rbacService.updatePermission(anyLong(), any(PermissionDto.class)))
                .thenThrow(new BusinessRuleException("Cannot remove the last account holding ROLE:WRITE"));

        JsonNode json = assertError(call(putJson("/api/v1/permissions/7", token("PERMISSION:WRITE"), VALID_BODY)), 422);

        assertThat(json.get("message").asText()).contains("last account");
    }

    @Test
    void _12_ShouldReturn200WithNoData_WhenPermissionIsDeleted() throws Exception {
        JsonNode json = assertSuccess(call(deleteWith("/api/v1/permissions/7", token("PERMISSION:WRITE"))), 200);

        assertThat(json.get("message").asText()).isEqualTo("Permission deleted");
        assertThat(json.get("data") == null || json.get("data").isNull()).isTrue();
        verify(rbacService).deletePermission(7L);
    }

    @Test
    void _13_ShouldReturn422_WhenPermissionIsStillAssignedToRoles() throws Exception {
        doThrow(new BusinessRuleException("Permission is still assigned to roles")).when(rbacService).deletePermission(7L);

        JsonNode json = assertError(call(deleteWith("/api/v1/permissions/7", token("PERMISSION:WRITE"))), 422);

        assertThat(json.get("message").asText()).isEqualTo("Permission is still assigned to roles");
    }

    @Test
    void _14_ShouldReturn404_WhenPermissionToDeleteDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Permission not found: 99")).when(rbacService).deletePermission(99L);

        assertError(call(deleteWith("/api/v1/permissions/99", token("PERMISSION:WRITE"))), 404);
    }
}
