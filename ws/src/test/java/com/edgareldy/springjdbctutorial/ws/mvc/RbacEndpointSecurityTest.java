package com.edgareldy.springjdbctutorial.ws.mvc;

import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.ws.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Permission matrix of the 14 RBAC endpoints of the README table, checked through the real filter chain and
 * method security: no token gives 401, a token holding every permission EXCEPT the required one gives 403, both
 * as ApiResponse errors and without ever reaching the (mocked) service. The 2xx paths with the right
 * permission are asserted in the three controller tests.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class RbacEndpointSecurityTest extends AbstractRbacMvcTest {

    private static final String ROLE_BODY = "{\"roleName\":\"Reviewer\"}";
    private static final String PERMISSION_BODY = "{\"resource\":\"INVOICE\",\"action\":\"READ\"}";

    // Valid bodies on purpose: request validation runs before the method security check, so an invalid body
    // would answer 400 and hide the permission rule under test.
    static Stream<Arguments> endpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/v1/users", null, "USER:READ"),
                Arguments.of(HttpMethod.GET, "/api/v1/users/1", null, "USER:READ"),
                Arguments.of(HttpMethod.PATCH, "/api/v1/users/1/roles/2", null, "USER:WRITE"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/users/1/roles/2", null, "USER:WRITE"),
                Arguments.of(HttpMethod.GET, "/api/v1/roles", null, "ROLE:READ"),
                Arguments.of(HttpMethod.POST, "/api/v1/roles", ROLE_BODY, "ROLE:WRITE"),
                Arguments.of(HttpMethod.PUT, "/api/v1/roles/2", ROLE_BODY, "ROLE:WRITE"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/roles/2", null, "ROLE:WRITE"),
                Arguments.of(HttpMethod.POST, "/api/v1/roles/2/permissions/3", null, "ROLE:WRITE"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/roles/2/permissions/3", null, "ROLE:WRITE"),
                Arguments.of(HttpMethod.GET, "/api/v1/permissions", null, "PERMISSION:READ"),
                Arguments.of(HttpMethod.POST, "/api/v1/permissions", PERMISSION_BODY, "PERMISSION:WRITE"),
                Arguments.of(HttpMethod.PUT, "/api/v1/permissions/2", PERMISSION_BODY, "PERMISSION:WRITE"),
                Arguments.of(HttpMethod.DELETE, "/api/v1/permissions/2", null, "PERMISSION:WRITE"));
    }

    private static MockHttpServletRequestBuilder request(HttpMethod method, String url, String body, String jwt) {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.request(method, url);
        if (jwt != null) {
            builder.header("Authorization", bearer(jwt));
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return builder;
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endpoints")
    void _01_ShouldReturn401ApiResponse_WhenNoTokenIsSent(HttpMethod method, String url, String body,
                                                          String required) throws Exception {
        MvcResult result = call(request(method, url, body, null));

        assertError(result, 401);
        verifyNoInteractions(rbacService);
    }

    @ParameterizedTest(name = "{0} {1} without {3}")
    @MethodSource("endpoints")
    void _02_ShouldReturn403ApiResponse_WhenTokenHoldsEveryPermissionExceptTheRequiredOne(
            HttpMethod method, String url, String body, String required) throws Exception {
        MvcResult result = call(request(method, url, body, tokenWithEverythingExcept(required)));

        JsonNode json = assertError(result, 403);
        assertThat(json.get("message").asText()).isEqualTo("Access denied");
        verifyNoInteractions(rbacService);
    }

    @ParameterizedTest(name = "{0} {1} with an empty token")
    @MethodSource("endpoints")
    void _03_ShouldReturn403ApiResponse_WhenTokenHoldsNoPermissionAtAll(HttpMethod method, String url, String body,
                                                                        String required) throws Exception {
        MvcResult result = call(request(method, url, body, token()));

        assertError(result, 403);
        verifyNoInteractions(rbacService);
    }

    @ParameterizedTest(name = "{0} {1} with a revoked token")
    @MethodSource("endpoints")
    void _04_ShouldReturn401_WhenTheTokenIsBlacklisted(HttpMethod method, String url, String body,
                                                       String required) throws Exception {
        String jwt = token(required);
        when(authService.isTokenBlacklisted(JwtService.sha256Hex(jwt))).thenReturn(true);

        assertError(call(request(method, url, body, jwt)), 401);
        verifyNoInteractions(rbacService);
    }
}
