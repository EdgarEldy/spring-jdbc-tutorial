package com.edgareldy.springjdbctutorial.ws.config;

import static org.assertj.core.api.Assertions.assertThat;
import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertError;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import com.edgareldy.springjdbctutorial.ws.support.AuthTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Full-stack integration test: the REAL WebMvcConfig (CommonConfig, DataSourceConfig, Flyway, auth services,
 * SecurityConfig, real service and DAO) against the Testcontainers PostgreSQL, driven through MockMvc.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// The PostgreSQL container class comes from the common module test-jar. @DynamicPropertySource injects
// its random JDBC url into the Environment before the real configuration is loaded.
@SpringJUnitWebConfig(WebMvcConfig.class)
class WebMvcConfigIntegrationTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // springSecurity() adds the springSecurityFilterChain bean in front of MVC, as the DelegatingFilterProxy
        // does in Tomcat, so the routes are really protected in these tests
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void _01_ShouldReturn200WithStatusUpWithoutToken_WhenHealthRouteIsCalledOnARealDatabase() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/health")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").get("status").asText()).isEqualTo("UP");
        assertThat(json.get("timestamp").asText()).isNotBlank();
    }

    @Test
    void _02_ShouldReturn401ApiResponse_WhenUrlIsUnknownAndNoTokenIsSent() throws Exception {
        assertError(mockMvc.perform(get("/api/v1/nothing-here")).andReturn(), 401);
    }

    @Test
    void _03_ShouldReturn404ApiResponse_WhenUrlIsUnknownAndTokenIsValid() throws Exception {
        try (AuthTestSupport support = new AuthTestSupport(mockMvc)) {
            String token = support.registerActivateAndLogin(AuthTestSupport.uniqueEmail("config"), AuthTestSupport.PASSWORD);

            assertError(support.getAs(token, "/api/v1/nothing-here"), 404);
        }
    }
}
