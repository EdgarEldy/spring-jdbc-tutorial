package com.edgareldy.springjdbctutorial.ws.mvc;

import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertError;
import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.auth.dto.UserDto;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import com.edgareldy.springjdbctutorial.ws.support.JwtTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/**
 * MockMvc tests of UserController with RbacService mocked (no database) but the real security filter chain,
 * method security and advice: status codes, ApiResponse shape, page/size validation and permissions.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class UserControllerTest extends AbstractRbacMvcTest {

    private static UserDto user(long id) {
        return new UserDto(id, "Alice", "Martin", "alice@example.com", "must-not-leak", true, false,
                List.of("ADMIN"), List.of("ROLE:WRITE"));
    }

    // ---------------------------------------------------------------- GET /users

    @Test
    void _01_ShouldReturn200WithPageShape_WhenTokenHoldsUserRead() throws Exception {
        when(rbacService.listUsers(0, 20)).thenReturn(new PageDto<>(List.of(user(1L)), 0, 20, 1L));

        JsonNode json = assertSuccess(call(getWith("/api/v1/users", token("USER:READ"))), 200);

        JsonNode data = json.get("data");
        assertThat(data.get("content")).hasSize(1);
        assertThat(data.get("content").get(0).get("email").asText()).isEqualTo("alice@example.com");
        assertThat(data.get("content").get(0).get("roles").get(0).asText()).isEqualTo("ADMIN");
        assertThat(data.get("content").get(0).has("password")).isFalse();
        assertThat(data.get("page").asInt()).isZero();
        assertThat(data.get("size").asInt()).isEqualTo(20);
        assertThat(data.get("totalElements").asLong()).isEqualTo(1L);
        assertThat(data.get("totalPages").asInt()).isEqualTo(1);
    }

    @Test
    void _02_ShouldForwardRequestedPageAndSize_WhenParametersAreGiven() throws Exception {
        when(rbacService.listUsers(2, 5)).thenReturn(new PageDto<>(List.of(), 2, 5, 11L));

        JsonNode json = assertSuccess(call(getWith("/api/v1/users?page=2&size=5", token("USER:READ"))), 200);

        verify(rbacService).listUsers(2, 5);
        assertThat(json.get("data").get("totalPages").asInt()).isEqualTo(3);
    }

    @Test
    void _03_ShouldKeepEmptyContentArrayInTheJson_WhenPageIsBeyondTheLastOne() throws Exception {
        when(rbacService.listUsers(99, 10)).thenReturn(new PageDto<>(List.of(), 99, 10, 3L));

        JsonNode json = assertSuccess(call(getWith("/api/v1/users?page=99&size=10", token("USER:READ"))), 200);

        JsonNode content = json.get("data").get("content");
        assertThat(content).isNotNull();
        assertThat(content.isArray()).isTrue();
        assertThat(content).isEmpty();
        assertThat(json.get("data").get("totalElements").asLong()).isEqualTo(3L);
    }

    @Test
    void _04_ShouldReturn400NamingThePageParameter_WhenPageIsNegative() throws Exception {
        JsonNode json = assertError(call(getWith("/api/v1/users?page=-1", token("USER:READ"))), 400);

        assertThat(json.get("message").asText()).contains("page: ").doesNotContain("size: ");
        verifyNoInteractions(rbacService);
    }

    @Test
    void _05_ShouldReturn400NamingTheSizeParameter_WhenSizeIsZero() throws Exception {
        JsonNode json = assertError(call(getWith("/api/v1/users?size=0", token("USER:READ"))), 400);

        assertThat(json.get("message").asText()).contains("size: ").doesNotContain("page: ");
        verifyNoInteractions(rbacService);
    }

    @Test
    void _06_ShouldReturn400NamingTheSizeParameter_WhenSizeIs101() throws Exception {
        JsonNode json = assertError(call(getWith("/api/v1/users?size=101", token("USER:READ"))), 400);

        assertThat(json.get("message").asText()).contains("size: ");
        verifyNoInteractions(rbacService);
    }

    @Test
    void _07_ShouldReturn400WithBothParameters_WhenPageAndSizeAreInvalid() throws Exception {
        JsonNode json = assertError(call(getWith("/api/v1/users?page=-5&size=500", token("USER:READ"))), 400);

        assertThat(json.get("message").asText()).contains("page: ").contains("size: ");
    }

    @Test
    void _08_ShouldAcceptTheBounds_WhenSizeIsOneAndOneHundred() throws Exception {
        when(rbacService.listUsers(0, 1)).thenReturn(new PageDto<>(List.of(), 0, 1, 0L));
        when(rbacService.listUsers(0, 100)).thenReturn(new PageDto<>(List.of(), 0, 100, 0L));

        assertSuccess(call(getWith("/api/v1/users?size=1", token("USER:READ"))), 200);
        assertSuccess(call(getWith("/api/v1/users?size=100", token("USER:READ"))), 200);
    }

    @Test
    void _09_ShouldReturn400_WhenPageIsNotANumber() throws Exception {
        JsonNode json = assertError(call(getWith("/api/v1/users?page=abc", token("USER:READ"))), 400);

        assertThat(json.get("message").asText()).isEqualTo("Invalid value for parameter: page");
    }

    // ---------------------------------------------------------------- GET /users/{id}

    @Test
    void _10_ShouldReturn200WithRolesAndNoPassword_WhenUserExists() throws Exception {
        when(rbacService.getUser(1L)).thenReturn(user(1L));

        JsonNode json = assertSuccess(call(getWith("/api/v1/users/1", token("USER:READ"))), 200);

        assertThat(json.get("data").get("id").asLong()).isEqualTo(1L);
        assertThat(json.get("data").get("roles").get(0).asText()).isEqualTo("ADMIN");
        assertThat(json.get("data").get("permissions").get(0).asText()).isEqualTo("ROLE:WRITE");
        assertThat(json.get("data").has("password")).isFalse();
    }

    @Test
    void _11_ShouldReturn404ApiResponse_WhenUserDoesNotExist() throws Exception {
        when(rbacService.getUser(99L)).thenThrow(new ResourceNotFoundException("User not found: 99"));

        JsonNode json = assertError(call(getWith("/api/v1/users/99", token("USER:READ"))), 404);

        assertThat(json.get("message").asText()).isEqualTo("User not found: 99");
    }

    @Test
    void _12_ShouldReturn400_WhenUserIdIsNotANumber() throws Exception {
        assertError(call(getWith("/api/v1/users/abc", token("USER:READ"))), 400);
    }

    // ---------------------------------------------------------------- PATCH /users/{id}/roles/{roleId}

    @Test
    void _13_ShouldReturn200_WhenTokenHoldsUserWriteAndRoleIsAssigned() throws Exception {
        when(rbacService.assignRoleToUser(1L, 3L)).thenReturn(user(1L));

        JsonNode json = assertSuccess(call(patchWith("/api/v1/users/1/roles/3", token("USER:WRITE"))), 200);

        assertThat(json.get("message").asText()).isEqualTo("Role assigned");
        assertThat(json.get("data").get("id").asLong()).isEqualTo(1L);
        verify(rbacService).assignRoleToUser(1L, 3L);
    }

    @Test
    void _14_ShouldReturn422_WhenRoleIsAlreadyAssigned() throws Exception {
        when(rbacService.assignRoleToUser(1L, 3L))
                .thenThrow(new BusinessRuleException("Role is already assigned to this user"));

        JsonNode json = assertError(call(patchWith("/api/v1/users/1/roles/3", token("USER:WRITE"))), 422);

        assertThat(json.get("message").asText()).isEqualTo("Role is already assigned to this user");
    }

    @Test
    void _15_ShouldReturn404_WhenUserOrRoleToAssignDoesNotExist() throws Exception {
        when(rbacService.assignRoleToUser(99L, 3L)).thenThrow(new ResourceNotFoundException("User not found: 99"));

        assertError(call(patchWith("/api/v1/users/99/roles/3", token("USER:WRITE"))), 404);
    }

    // ---------------------------------------------------------------- DELETE /users/{id}/roles/{roleId}

    @Test
    void _16_ShouldReturn200_WhenTokenHoldsUserWriteAndRoleIsRemoved() throws Exception {
        when(rbacService.removeRoleFromUser(1L, 3L)).thenReturn(user(1L));

        JsonNode json = assertSuccess(call(deleteWith("/api/v1/users/1/roles/3", token("USER:WRITE"))), 200);

        assertThat(json.get("message").asText()).isEqualTo("Role removed");
        verify(rbacService).removeRoleFromUser(1L, 3L);
    }

    @Test
    void _17_ShouldReturn422_WhenRemovalWouldLeaveNoAdmin() throws Exception {
        when(rbacService.removeRoleFromUser(1L, 3L))
                .thenThrow(new BusinessRuleException("Cannot remove the last account holding ROLE:WRITE"));

        JsonNode json = assertError(call(deleteWith("/api/v1/users/1/roles/3", token("USER:WRITE"))), 422);

        assertThat(json.get("message").asText()).contains("last account");
    }

    @Test
    void _18_ShouldReturn404_WhenRoleIsNotAssignedToTheUser() throws Exception {
        when(rbacService.removeRoleFromUser(1L, 3L))
                .thenThrow(new ResourceNotFoundException("Role 3 is not assigned to user 1"));

        assertError(call(deleteWith("/api/v1/users/1/roles/3", token("USER:WRITE"))), 404);
    }

    // ---------------------------------------------------------------- errors common to the routes

    @Test
    void _19_ShouldReturn500WithGenericMessage_WhenServiceThrowsAnUnexpectedException() throws Exception {
        when(rbacService.getUser(1L)).thenThrow(new IllegalStateException("secret internal detail"));

        JsonNode json = assertError(call(getWith("/api/v1/users/1", token("USER:READ"))), 500);

        assertThat(json.get("message").asText()).isEqualTo("Internal server error");
        assertThat(json.toString()).doesNotContain("secret internal detail");
    }

    @Test
    void _20_ShouldReturn405ApiResponse_WhenAnUnsupportedMethodIsUsedOnTheCollection() throws Exception {
        MvcResult result = call(postWith("/api/v1/users", token(allPermissions())));

        assertError(result, 405);
    }

    private static String[] allPermissions() {
        return JwtTestSupport.ALL_PERMISSIONS.toArray(new String[0]);
    }
}
