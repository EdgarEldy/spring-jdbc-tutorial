package com.edgareldy.springjdbctutorial.ws.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UnsupportedEncodingException;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Shared assertions on the ApiResponse envelope, so every test checks the README error format the same way.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
public final class ApiAssertions {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ApiAssertions() {
    }

    /** Parses the response body as JSON. */
    public static JsonNode json(MvcResult result) throws UnsupportedEncodingException {
        try {
            return MAPPER.readTree(result.getResponse().getContentAsString());
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new AssertionError("Response body is not JSON: " + result.getResponse().getContentAsString(), e);
        }
    }

    /**
     * Asserts the README error format: the expected status, success=false, a message, no data and a timestamp.
     * Returns the body for further assertions on the message.
     */
    public static JsonNode assertError(MvcResult result, int expectedStatus) throws UnsupportedEncodingException {
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        JsonNode json = json(result);
        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("message").asText()).isNotBlank();
        assertThat(json.get("data") == null || json.get("data").isNull()).isTrue();
        assertThat(json.get("timestamp").asText()).isNotBlank();
        return json;
    }

    /** Asserts a success envelope with the expected status and returns the body. */
    public static JsonNode assertSuccess(MvcResult result, int expectedStatus) throws UnsupportedEncodingException {
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        JsonNode json = json(result);
        assertThat(json.get("success").asBoolean()).isTrue();
        assertThat(json.get("message").asText()).isNotBlank();
        assertThat(json.get("timestamp").asText()).isNotBlank();
        return json;
    }
}
