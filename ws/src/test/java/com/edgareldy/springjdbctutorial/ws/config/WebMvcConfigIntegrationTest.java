package com.edgareldy.springjdbctutorial.ws.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
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
 * Full-stack integration test: the REAL WebMvcConfig (CommonConfig, DataSourceConfig, Flyway, real
 * service and DAO) against the Testcontainers PostgreSQL, driven through MockMvc.
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
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void _01_ShouldReturn200WithStatusUp_WhenHealthRouteIsCalledOnARealDatabase() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/health")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").get("status").asText()).isEqualTo("UP");
        assertThat(json.get("timestamp").asText()).isNotBlank();
    }

    @Test
    void _02_ShouldReturn404ApiResponse_WhenUrlIsUnknown() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/nothing-here")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("message").asText()).isNotBlank();
        assertThat(json.get("data") == null || json.get("data").isNull()).isTrue();
        assertThat(json.get("timestamp").asText()).isNotBlank();
    }
}
