package com.edgareldy.springjdbctutorial.ws.mvc;

import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.order.dto.OrderDto;
import com.edgareldy.springjdbctutorial.ws.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
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
 * Permission matrix of the 3 order endpoints of the README table, checked through the real filter chain and method security: no token or a revoked token gives 401, a token holding every permission EXCEPT the required one (or none at all) gives 403, all as ApiResponse errors and without reaching the (mocked) service; the right permission alone gives 2xx.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
class OrderEndpointSecurityTest extends AbstractOrderMvcTest {

    private static final String BODY = "{\"customerId\":2,\"productId\":3,\"quantity\":1}";

    // Valid bodies on purpose: request validation runs before the method security check, so an invalid body
    // would answer 400 and hide the permission rule under test.
    static Stream<Arguments> endpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/v1/orders", null, "ORDER:READ"),
                Arguments.of(HttpMethod.GET, "/api/v1/orders/1", null, "ORDER:READ"),
                Arguments.of(HttpMethod.POST, "/api/v1/orders", BODY, "ORDER:WRITE"));
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
        assertError(call(request(method, url, body, null)), 401);
        verifyNoInteractions(orderService);
    }

    @ParameterizedTest(name = "{0} {1} without {3}")
    @MethodSource("endpoints")
    void _02_ShouldReturn403ApiResponse_WhenTokenHoldsEveryPermissionExceptTheRequiredOne(
            HttpMethod method, String url, String body, String required) throws Exception {
        MvcResult result = call(request(method, url, body, tokenWithEverythingExcept(required)));

        JsonNode json = assertError(result, 403);
        assertThat(json.get("message").asText()).isEqualTo("Access denied");
        verifyNoInteractions(orderService);
    }

    @ParameterizedTest(name = "{0} {1} with an empty token")
    @MethodSource("endpoints")
    void _03_ShouldReturn403ApiResponse_WhenTokenHoldsNoPermissionAtAll(HttpMethod method, String url, String body,
                                                                        String required) throws Exception {
        assertError(call(request(method, url, body, token())), 403);
        verifyNoInteractions(orderService);
    }

    @ParameterizedTest(name = "{0} {1} with a revoked token")
    @MethodSource("endpoints")
    void _04_ShouldReturn401_WhenTheTokenIsBlacklisted(HttpMethod method, String url, String body,
                                                       String required) throws Exception {
        String jwt = token(required);
        when(authService.isTokenBlacklisted(JwtService.sha256Hex(jwt))).thenReturn(true);

        assertError(call(request(method, url, body, jwt)), 401);
        verifyNoInteractions(orderService);
    }

    @ParameterizedTest(name = "{0} {1} with {3}")
    @MethodSource("endpoints")
    void _05_ShouldNotReturn401Or403_WhenTokenHoldsOnlyTheRequiredPermission(HttpMethod method, String url,
                                                                            String body, String required)
            throws Exception {
        // Mocks return null, which would make the controller fail on the converter, so give the paths a real
        // answer; the point of this test is only that the permission opens the door.
        OrderDto order = new OrderDto(1L, 2L, 3L, 1, new BigDecimal("10.00"));
        when(orderService.list(0, 20, null, null)).thenReturn(new PageDto<OrderDto>(List.of(), 0, 20, 0));
        when(orderService.get(1L)).thenReturn(order);
        when(orderService.create(any())).thenReturn(order);

        MvcResult result = call(request(method, url, body, token(required)));

        assertThat(result.getResponse().getStatus()).isIn(200, 201);
    }
}
