package com.edgareldy.springjdbctutorial.ws.mvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import com.edgareldy.springjdbctutorial.ws.config.WebMvcConfig;
import com.edgareldy.springjdbctutorial.ws.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Verifies that GlobalExceptionHandler turns every exception into the ApiResponse error format with
 * the right HTTP status, through MockMvc and a test-only controller that throws each case.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// These MVC tests live in ws.mvc, NOT in ws.controller/ws.exception: WebMvcConfig component-scans those
// two packages, and it would pick up the test-only configuration and controllers found on the test classpath.
// MockMvc drives the DispatcherServlet in memory (no HTTP port). @SpringJUnitWebConfig builds a
// WebApplicationContext from the nested TestConfig; webAppContextSetup then wires MockMvc on it, so
// the real handler mappings, argument resolvers, validator and @RestControllerAdvice all run.
@SpringJUnitWebConfig(GlobalExceptionHandlerTest.TestConfig.class)
class GlobalExceptionHandlerTest {

    /**
     * Test context: MVC enabled, the real advice, the throwing controller, and the converters and
     * validator of the real WebMvcConfig so JSON and validation behave as in production.
     */
    @Configuration
    @EnableWebMvc
    static class TestConfig implements WebMvcConfigurer {

        private final WebMvcConfig real = new WebMvcConfig();

        @Bean
        GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }

        @Bean
        ThrowingController throwingController() {
            return new ThrowingController();
        }

        @Override
        public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
            real.configureMessageConverters(converters);
        }

        @Override
        public Validator getValidator() {
            return real.getValidator();
        }
    }

    /**
     * Test-only controller whose endpoints throw or validate to trigger each handler.
     */
    @RestController
    @RequestMapping("/test")
    static class ThrowingController {

        @GetMapping("/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Category 5 not found");
        }

        @GetMapping("/business")
        String business() {
            throw new BusinessRuleException("Category is still referenced");
        }

        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("secret internal detail");
        }

        // A Spring ErrorResponse exception without a dedicated handler, carrying framework wording
        @GetMapping("/error-response")
        String errorResponse() {
            org.springframework.web.ErrorResponseException e = new org.springframework.web.ErrorResponseException(
                    org.springframework.http.HttpStatus.CONFLICT);
            e.getBody().setDetail("secret framework detail");
            throw e;
        }

        @PostMapping(value = "/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        String body(@Valid @RequestBody SampleBody body) {
            return body.getName();
        }

        @GetMapping("/param")
        String param(@RequestParam int count) {
            return String.valueOf(count);
        }

        // Constraint annotations directly on a parameter trigger Spring 6.1+ method validation
        @GetMapping("/range")
        String range(@RequestParam @Min(1) @Max(100) int size) {
            return String.valueOf(size);
        }
    }

    /**
     * Request body validated by the test endpoint.
     */
    public static class SampleBody {

        @NotBlank
        private String name;

        @Min(1)
        private int age = 1;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    /** Asserts the README error shape and returns the parsed body. */
    private JsonNode assertErrorShape(MvcResult result, int status) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        JsonNode json = mapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("message").asText()).isNotBlank();
        assertThat(json.get("data") == null || json.get("data").isNull()).isTrue();
        assertThat(json.get("timestamp").asText()).isNotBlank();
        return json;
    }

    @Test
    void _01_ShouldReturn404_WhenResourceNotFoundExceptionIsThrown() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/not-found")).andReturn();

        assertThat(assertErrorShape(result, 404).get("message").asText()).isEqualTo("Category 5 not found");
    }

    @Test
    void _02_ShouldReturn422_WhenBusinessRuleExceptionIsThrown() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/business")).andReturn();

        assertThat(assertErrorShape(result, 422).get("message").asText()).isEqualTo("Category is still referenced");
    }

    @Test
    void _03_ShouldReturn400WithPerFieldMessages_WhenBodyValidationFails() throws Exception {
        MvcResult result = mockMvc.perform(post("/test/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\" \",\"age\":0}")).andReturn();

        String message = assertErrorShape(result, 400).get("message").asText();
        assertThat(message).contains("name: must not be blank").contains("age: must be greater than or equal to 1");
    }

    @Test
    void _04_ShouldReturn400_WhenJsonIsMalformed() throws Exception {
        MvcResult result = mockMvc.perform(post("/test/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not json")).andReturn();

        assertThat(assertErrorShape(result, 400).get("message").asText()).isEqualTo("Malformed JSON request");
    }

    @Test
    void _05_ShouldReturn400_WhenRequiredParameterIsMissing() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/param")).andReturn();

        assertThat(assertErrorShape(result, 400).get("message").asText()).contains("count");
    }

    @Test
    void _06_ShouldReturn400_WhenParameterHasTheWrongType() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/param").param("count", "abc")).andReturn();

        assertThat(assertErrorShape(result, 400).get("message").asText()).contains("count");
    }

    @Test
    void _07_ShouldReturn400_WhenParameterIsBelowMin() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/range").param("size", "0")).andReturn();

        assertThat(assertErrorShape(result, 400).get("message").asText())
                .contains("must be greater than or equal to 1");
    }

    @Test
    void _08_ShouldReturn400_WhenParameterIsAboveMax() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/range").param("size", "101")).andReturn();

        assertThat(assertErrorShape(result, 400).get("message").asText())
                .contains("must be less than or equal to 100");
    }

    @Test
    void _09_ShouldReturn404_WhenUrlIsUnknown() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/does-not-exist")).andReturn();

        assertErrorShape(result, 404);
    }

    @Test
    void _10_ShouldReturn405WithAllowHeader_WhenMethodIsNotSupported() throws Exception {
        MvcResult result = mockMvc.perform(post("/test/not-found")).andReturn();

        assertErrorShape(result, 405);
        assertThat(result.getResponse().getHeader("Allow")).contains("GET");
    }

    @Test
    void _11_ShouldReturn415_WhenContentTypeIsNotSupported() throws Exception {
        MvcResult result = mockMvc.perform(post("/test/body")
                .contentType(MediaType.TEXT_PLAIN)
                .content("name")).andReturn();

        assertErrorShape(result, 415);
    }

    @Test
    void _12_ShouldReturn500WithGenericMessage_WhenUnexpectedExceptionIsThrown() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/boom")).andReturn();

        JsonNode json = assertErrorShape(result, 500);
        assertThat(json.get("message").asText()).isEqualTo("Internal server error");
        assertThat(result.getResponse().getContentAsString()).doesNotContain("secret internal detail");
    }

    // Non-regression: the catch-all used to copy the detail text of any ErrorResponse to the client
    @Test
    void _13_ShouldReturnReasonPhraseOnly_WhenAnErrorResponseExceptionHasNoDedicatedHandler() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/error-response")).andReturn();

        JsonNode json = assertErrorShape(result, 409);
        assertThat(json.get("message").asText()).isEqualTo("Conflict");
        assertThat(result.getResponse().getContentAsString()).doesNotContain("secret framework detail");
    }
}
