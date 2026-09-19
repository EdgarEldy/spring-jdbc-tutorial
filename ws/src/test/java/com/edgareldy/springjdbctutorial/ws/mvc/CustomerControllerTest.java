package com.edgareldy.springjdbctutorial.ws.mvc;

import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertError;
import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import com.edgareldy.springjdbctutorial.core.customer.dto.CustomerDto;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * MockMvc tests of CustomerController with CustomerService mocked but the real security and validation: statuses,
 * ApiResponse shape, page/size bounds, per-field body validation and the 404/422 refusals.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
class CustomerControllerTest extends AbstractCustomerMvcTest {

    private static final String READ = "CUSTOMER:READ";
    private static final String WRITE = "CUSTOMER:WRITE";
    private static final String URL = "/api/v1/customers";
    private static final String VALID_BODY = "{\"firstName\":\"Alice\",\"lastName\":\"Martin\","
            + "\"telephone\":\"555\",\"email\":\"alice@example.com\",\"address\":\"1 Main Street\"}";
    private static final CustomerDto ALICE = new CustomerDto(1L, "Alice", "Martin", "555", "alice@example.com", "1 Main Street");

    private static String body(String firstName, String lastName, String email) {
        return "{\"firstName\":\"" + firstName + "\",\"lastName\":\"" + lastName + "\",\"email\":\"" + email + "\"}";
    }

    // ---------------------------------------------------------------- GET /customers

    @Test
    void _01_ShouldReturn200WithPageAndDefaults_WhenTokenHoldsCustomerRead() throws Exception {
        when(customerService.list(0, 20)).thenReturn(new PageDto<>(List.of(ALICE), 0, 20, 1));

        JsonNode data = assertSuccess(call(getWith(URL, token(READ))), 200).get("data");

        JsonNode first = data.get("content").get(0);
        assertThat(first.get("id").asLong()).isEqualTo(1L);
        assertThat(first.get("firstName").asText()).isEqualTo("Alice");
        assertThat(first.get("lastName").asText()).isEqualTo("Martin");
        assertThat(first.get("telephone").asText()).isEqualTo("555");
        assertThat(first.get("email").asText()).isEqualTo("alice@example.com");
        assertThat(first.get("address").asText()).isEqualTo("1 Main Street");
        assertThat(data.get("page").asInt()).isZero();
        assertThat(data.get("size").asInt()).isEqualTo(20);
        assertThat(data.get("totalElements").asLong()).isEqualTo(1L);
        assertThat(data.get("totalPages").asInt()).isEqualTo(1);
    }

    @Test
    void _02_ShouldPassPageAndSizeToTheService_WhenTheyAreGiven() throws Exception {
        when(customerService.list(2, 5)).thenReturn(new PageDto<>(List.of(), 2, 5, 11));

        JsonNode json = assertSuccess(call(getWith(URL + "?page=2&size=5", token(READ))), 200);

        assertThat(json.get("data").get("page").asInt()).isEqualTo(2);
        assertThat(json.get("data").get("size").asInt()).isEqualTo(5);
        verify(customerService).list(2, 5);
    }

    @Test
    void _03_ShouldKeepEmptyContentInTheJson_WhenPageIsBeyondTheLastOne() throws Exception {
        when(customerService.list(99, 10)).thenReturn(new PageDto<>(List.of(), 99, 10, 3));

        JsonNode data = assertSuccess(call(getWith(URL + "?page=99&size=10", token(READ))), 200).get("data");

        assertThat(data.has("content")).isTrue();
        assertThat(data.get("content").isArray()).isTrue();
        assertThat(data.get("content")).isEmpty();
        assertThat(data.get("totalElements").asLong()).isEqualTo(3L);
    }

    @Test
    void _04_ShouldReturn400NamingPage_WhenPageIsNegative() throws Exception {
        JsonNode json = assertError(call(getWith(URL + "?page=-1", token(READ))), 400);

        assertThat(json.get("message").asText()).contains("page: ").doesNotContain("size: ");
        verifyNoInteractions(customerService);
    }

    @Test
    void _05_ShouldReturn400NamingSize_WhenSizeIsZeroOrAbove100() throws Exception {
        JsonNode zero = assertError(call(getWith(URL + "?size=0", token(READ))), 400);
        JsonNode big = assertError(call(getWith(URL + "?size=101", token(READ))), 400);

        assertThat(zero.get("message").asText()).contains("size: ").doesNotContain("page: ");
        assertThat(big.get("message").asText()).contains("size: ");
        verifyNoInteractions(customerService);
    }

    @Test
    void _06_ShouldReturn400NamingBoth_WhenPageAndSizeAreInvalid() throws Exception {
        JsonNode json = assertError(call(getWith(URL + "?page=-5&size=500", token(READ))), 400);

        assertThat(json.get("message").asText()).contains("page: ").contains("size: ");
    }

    @Test
    void _07_ShouldAcceptBoundarySizes_WhenSizeIs1Or100() throws Exception {
        when(customerService.list(0, 1)).thenReturn(new PageDto<>(List.of(), 0, 1, 0));
        when(customerService.list(0, 100)).thenReturn(new PageDto<>(List.of(), 0, 100, 0));

        assertSuccess(call(getWith(URL + "?size=1", token(READ))), 200);
        assertSuccess(call(getWith(URL + "?size=100", token(READ))), 200);
    }

    // ---------------------------------------------------------------- GET /customers/{id}

    @Test
    void _08_ShouldReturn200WithNullOptionalFields_WhenCustomerHasNoOptionalData() throws Exception {
        when(customerService.get(2L)).thenReturn(new CustomerDto(2L, "Bob", "Stone", null, null, null));

        JsonNode json = assertSuccess(call(getWith(URL + "/2", token(READ))), 200);

        assertThat(json.get("data").get("id").asLong()).isEqualTo(2L);
        assertThat(json.get("data").get("firstName").asText()).isEqualTo("Bob");
        assertThat(json.get("data").get("telephone").isNull()).isTrue();
        assertThat(json.get("data").get("email").isNull()).isTrue();
        assertThat(json.get("data").get("address").isNull()).isTrue();
    }

    @Test
    void _09_ShouldReturn404ApiResponse_WhenCustomerDoesNotExist() throws Exception {
        when(customerService.get(99L)).thenThrow(new ResourceNotFoundException("Customer not found: 99"));

        JsonNode json = assertError(call(getWith(URL + "/99", token(READ))), 404);

        assertThat(json.get("message").asText()).isEqualTo("Customer not found: 99");
    }

    @Test
    void _10_ShouldReturn400_WhenCustomerIdIsNotANumber() throws Exception {
        assertError(call(getWith(URL + "/abc", token(READ))), 400);
        verifyNoInteractions(customerService);
    }

    // ---------------------------------------------------------------- POST /customers

    @Test
    void _11_ShouldReturn201WithCreatedCustomer_WhenBodyIsValid() throws Exception {
        when(customerService.create(any(CustomerDto.class))).thenReturn(ALICE);

        JsonNode json = assertSuccess(call(postJson(URL, token(WRITE), VALID_BODY)), 201);

        assertThat(json.get("data").get("id").asLong()).isEqualTo(1L);
        assertThat(json.get("message").asText()).isEqualTo("Customer created");
        ArgumentCaptor<CustomerDto> sent = ArgumentCaptor.forClass(CustomerDto.class);
        verify(customerService).create(sent.capture());
        assertThat(sent.getValue()).isEqualTo(new CustomerDto(null, "Alice", "Martin", "555", "alice@example.com", "1 Main Street"));
    }

    @Test
    void _12_ShouldReturn201_WhenOptionalFieldsAreAbsentOrEmpty() throws Exception {
        when(customerService.create(any(CustomerDto.class))).thenReturn(new CustomerDto(3L, "Bob", "Stone", null, null, null));

        assertSuccess(call(postJson(URL, token(WRITE), "{\"firstName\":\"Bob\",\"lastName\":\"Stone\"}")), 201);
        assertSuccess(call(postJson(URL, token(WRITE),
                "{\"firstName\":\"Bob\",\"lastName\":\"Stone\",\"telephone\":\"\",\"email\":\"\",\"address\":\"\"}")), 201);
    }

    @Test
    void _13_ShouldReturn400NamingTheField_WhenFirstOrLastNameIsBlankOrMissing() throws Exception {
        JsonNode first = assertError(call(postJson(URL, token(WRITE), body("  ", "Martin", "a@b.co"))), 400);
        JsonNode last = assertError(call(postJson(URL, token(WRITE), body("Alice", "", "a@b.co"))), 400);
        JsonNode missing = assertError(call(postJson(URL, token(WRITE), "{}")), 400);

        assertThat(first.get("message").asText()).contains("firstName: ").doesNotContain("lastName: ");
        assertThat(last.get("message").asText()).contains("lastName: ").doesNotContain("firstName: ");
        assertThat(missing.get("message").asText()).contains("firstName: ").contains("lastName: ");
        verifyNoInteractions(customerService);
    }

    @Test
    void _14_ShouldReturn400NamingTheField_WhenEmailIsMalformed() throws Exception {
        JsonNode json = assertError(call(postJson(URL, token(WRITE), body("Alice", "Martin", "not-an-email"))), 400);

        assertThat(json.get("message").asText()).contains("email: ");
        verifyNoInteractions(customerService);
    }

    @Test
    void _15_ShouldReturn400NamingTheField_WhenAFieldIsOversize() throws Exception {
        String longName = "x".repeat(101);
        JsonNode first = assertError(call(postJson(URL, token(WRITE), body(longName, "Martin", ""))), 400);
        JsonNode last = assertError(call(postJson(URL, token(WRITE), body("Alice", longName, ""))), 400);
        JsonNode phone = assertError(call(postJson(URL, token(WRITE),
                "{\"firstName\":\"A\",\"lastName\":\"B\",\"telephone\":\"" + "1".repeat(31) + "\"}")), 400);
        JsonNode address = assertError(call(postJson(URL, token(WRITE),
                "{\"firstName\":\"A\",\"lastName\":\"B\",\"address\":\"" + "a".repeat(256) + "\"}")), 400);
        JsonNode email = assertError(call(postJson(URL, token(WRITE),
                body("Alice", "Martin", "a".repeat(250) + "@b.com"))), 400);

        assertThat(first.get("message").asText()).contains("firstName: ");
        assertThat(last.get("message").asText()).contains("lastName: ");
        assertThat(phone.get("message").asText()).contains("telephone: ");
        assertThat(address.get("message").asText()).contains("address: ");
        assertThat(email.get("message").asText()).contains("email: ");
        verifyNoInteractions(customerService);
    }

    @Test
    void _16_ShouldReturn201_WhenFieldsSitExactlyOnTheirLimits() throws Exception {
        when(customerService.create(any(CustomerDto.class))).thenReturn(ALICE);
        String limits = "{\"firstName\":\"" + "x".repeat(100) + "\",\"lastName\":\"" + "y".repeat(100)
                + "\",\"telephone\":\"" + "1".repeat(30) + "\",\"address\":\"" + "a".repeat(255) + "\"}";

        assertSuccess(call(postJson(URL, token(WRITE), limits)), 201);
    }

    @Test
    void _17_ShouldReturn400_WhenBodyIsMalformedJson() throws Exception {
        assertError(call(postJson(URL, token(WRITE), "{\"firstName\":")), 400);
        verifyNoInteractions(customerService);
    }

    @Test
    void _18_ShouldReturn422_WhenServiceRefusesTheEmailFormat() throws Exception {
        when(customerService.create(any(CustomerDto.class))).thenThrow(new BusinessRuleException("Email is not valid"));

        JsonNode json = assertError(call(postJson(URL, token(WRITE), body("Alice", "Martin", "a@b"))), 422);

        assertThat(json.get("message").asText()).isEqualTo("Email is not valid");
    }

    @Test
    void _19_ShouldReturn415_WhenContentTypeIsNotJson() throws Exception {
        assertError(call(MockMvcRequestBuilders.post(URL)
                .header("Authorization", bearer(token(WRITE))).contentType(MediaType.TEXT_PLAIN).content("Alice")), 415);
        verifyNoInteractions(customerService);
    }

    // ---------------------------------------------------------------- PUT /customers/{id}

    @Test
    void _20_ShouldReturn200WithUpdatedCustomer_WhenBodyIsValid() throws Exception {
        when(customerService.update(eq(1L), any(CustomerDto.class))).thenReturn(ALICE);

        JsonNode json = assertSuccess(call(putJson(URL + "/1", token(WRITE), VALID_BODY)), 200);

        assertThat(json.get("data").get("firstName").asText()).isEqualTo("Alice");
        assertThat(json.get("message").asText()).isEqualTo("Customer updated");
    }

    @Test
    void _21_ShouldReturn404ApiResponse_WhenUpdatingAnUnknownCustomer() throws Exception {
        when(customerService.update(eq(99L), any(CustomerDto.class)))
                .thenThrow(new ResourceNotFoundException("Customer not found: 99"));

        assertError(call(putJson(URL + "/99", token(WRITE), VALID_BODY)), 404);
    }

    @Test
    void _22_ShouldReturn400_WhenUpdatedBodyIsInvalid() throws Exception {
        JsonNode json = assertError(call(putJson(URL + "/1", token(WRITE), body("", "Martin", "bad"))), 400);

        assertThat(json.get("message").asText()).contains("firstName: ").contains("email: ");
        verifyNoInteractions(customerService);
    }

    // ---------------------------------------------------------------- DELETE /customers/{id}

    @Test
    void _23_ShouldReturn200WithoutData_WhenCustomerIsDeleted() throws Exception {
        JsonNode json = assertSuccess(call(deleteWith(URL + "/2", token(WRITE))), 200);

        assertThat(json.get("message").asText()).isEqualTo("Customer deleted");
        assertThat(json.get("data") == null || json.get("data").isNull()).isTrue();
        verify(customerService).delete(2L);
    }

    @Test
    void _24_ShouldReturn404ApiResponse_WhenDeletingAnUnknownCustomer() throws Exception {
        doThrow(new ResourceNotFoundException("Customer not found: 99")).when(customerService).delete(99L);

        assertError(call(deleteWith(URL + "/99", token(WRITE))), 404);
    }

    @Test
    void _25_ShouldReturn422_WhenCustomerIsReferencedByOrders() throws Exception {
        doThrow(new BusinessRuleException("Customer is referenced by orders")).when(customerService).delete(2L);

        JsonNode json = assertError(call(deleteWith(URL + "/2", token(WRITE))), 422);

        assertThat(json.get("message").asText()).isEqualTo("Customer is referenced by orders");
    }

    // ---------------------------------------------------------------- unsupported method

    @Test
    void _26_ShouldReturn405ApiResponse_WhenPatchIsUsedOnACustomer() throws Exception {
        assertError(call(patchWith(URL + "/2", token(WRITE))), 405);
    }
}
