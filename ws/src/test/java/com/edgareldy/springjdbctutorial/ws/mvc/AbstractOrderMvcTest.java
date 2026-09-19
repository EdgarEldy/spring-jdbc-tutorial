package com.edgareldy.springjdbctutorial.ws.mvc;

import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.edgareldy.springjdbctutorial.core.auth.service.AuthService;
import com.edgareldy.springjdbctutorial.core.order.service.OrderService;
import com.edgareldy.springjdbctutorial.ws.support.JwtTestSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Common base of the order controller tests: builds MockMvc on the shared OrderMvcTestConfig context, resets
 * the mocked services before each test and offers request helpers (signed tokens, JSON bodies).
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
// MockMvc + springSecurity() runs the real filter chain in front of the DispatcherServlet (same as
// AbstractCatalogMvcTest): the JWT filter and @PreAuthorize are exercised, only the services are mocked.
@SpringJUnitWebConfig(OrderMvcTestConfig.class)
abstract class AbstractOrderMvcTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    protected OrderService orderService;
    @Autowired
    protected AuthService authService;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        reset(orderService, authService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /** A signed dev-key JWT carrying exactly these permissions. */
    protected static String token(String... permissions) {
        return JwtTestSupport.tokenWithPermissions(5L, List.of(permissions));
    }

    /** A signed dev-key JWT carrying every seeded permission except the given one. */
    protected static String tokenWithEverythingExcept(String permission) {
        return JwtTestSupport.tokenWithPermissions(5L,
                JwtTestSupport.ALL_PERMISSIONS.stream().filter(p -> !p.equals(permission)).toList());
    }

    protected static String bearer(String jwt) {
        return "Bearer " + jwt;
    }

    protected MvcResult call(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andReturn();
    }

    protected static MockHttpServletRequestBuilder getWith(String url, String jwt) {
        return get(url).header("Authorization", bearer(jwt));
    }

    protected static MockHttpServletRequestBuilder deleteWith(String url, String jwt) {
        return delete(url).header("Authorization", bearer(jwt));
    }

    protected static MockHttpServletRequestBuilder patchWith(String url, String jwt) {
        return patch(url).header("Authorization", bearer(jwt));
    }

    protected static MockHttpServletRequestBuilder postJson(String url, String jwt, String body) {
        return post(url).header("Authorization", bearer(jwt)).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected static MockHttpServletRequestBuilder putJson(String url, String jwt, String body) {
        return put(url).header("Authorization", bearer(jwt)).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
