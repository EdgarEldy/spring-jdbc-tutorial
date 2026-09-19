package com.edgareldy.springjdbctutorial.ws.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.edgareldy.springjdbctutorial.core.common.dto.HealthDto;
import com.edgareldy.springjdbctutorial.core.common.service.HealthService;
import com.edgareldy.springjdbctutorial.ws.config.WebMvcConfig;
import com.edgareldy.springjdbctutorial.ws.controller.HealthController;
import com.edgareldy.springjdbctutorial.ws.exception.GlobalExceptionHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MockMvc tests of HealthController with the HealthService mocked (no database).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// Kept out of ws.controller on purpose: the component scan of WebMvcConfig would pick up this test config.
@SpringJUnitWebConfig(HealthControllerTest.TestConfig.class)
class HealthControllerTest {

    /**
     * Test context: MVC, the real controller and advice, and a Mockito mock in place of the service.
     */
    @Configuration
    @EnableWebMvc
    static class TestConfig implements WebMvcConfigurer {

        @Bean
        HealthService healthService() {
            return mock(HealthService.class);
        }

        @Bean
        HealthController healthController(HealthService healthService) {
            return new HealthController(healthService);
        }

        @Bean
        GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }

        @Override
        public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
            new WebMvcConfig().configureMessageConverters(converters);
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private HealthService healthService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        reset(healthService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void _01_ShouldReturn200WithStatusUp_WhenServiceReportsUp() throws Exception {
        when(healthService.check()).thenReturn(new HealthDto("UP", "UP"));

        MvcResult result = mockMvc.perform(get("/api/v1/health")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("data").get("status").asText()).isEqualTo("UP");
        assertThat(json.get("data").get("database").asText()).isEqualTo("UP");
        assertThat(json.get("timestamp").asText()).isNotBlank();
    }

    @Test
    void _02_ShouldExposeStatusDown_WhenServiceReportsDown() throws Exception {
        when(healthService.check()).thenReturn(new HealthDto("DOWN", "DOWN"));

        MvcResult result = mockMvc.perform(get("/api/v1/health")).andReturn();

        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("data").get("status").asText()).isEqualTo("DOWN");
    }

    @Test
    void _03_ShouldReturn405WithErrorShape_WhenMethodIsPost() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/health")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(405);
        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("success").asBoolean()).isFalse();
    }
}
