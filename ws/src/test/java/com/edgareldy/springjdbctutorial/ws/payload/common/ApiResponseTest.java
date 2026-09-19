package com.edgareldy.springjdbctutorial.ws.payload.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

/**
 * Unit tests of the ApiResponse envelope (success and error shapes).
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
class ApiResponseTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void _01_ShouldCarryDataAndMessage_WhenBuildingASuccess() {
        ApiResponse<String> response = ApiResponse.success("payload", "done");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("payload");
        assertThat(response.getMessage()).isEqualTo("done");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void _02_ShouldHaveNoData_WhenBuildingAnError() {
        ApiResponse<Void> response = ApiResponse.error("boom");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("boom");
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void _03_ShouldSerializeTheFourFields_WhenWritingAnErrorAsJson() throws Exception {
        JsonNode json = mapper.readTree(mapper.writeValueAsString(ApiResponse.error("boom")));

        assertThat(json.get("success").asBoolean()).isFalse();
        assertThat(json.get("message").asText()).isEqualTo("boom");
        assertThat(json.get("data").isNull()).isTrue();
        assertThat(json.get("timestamp").asText()).isNotBlank();
    }
}
