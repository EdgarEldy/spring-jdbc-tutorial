package com.edgareldy.springjdbctutorial.ws.mvc;

import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertError;
import static com.edgareldy.springjdbctutorial.ws.support.ApiAssertions.assertSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import com.edgareldy.springjdbctutorial.core.order.dto.OrderDto;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * MockMvc tests of OrderController with OrderService mocked but the real security and validation: statuses, ApiResponse shape, filters, page/size bounds, per-field body validation, the 404/422 refusals and the missing PUT/DELETE.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
class OrderControllerTest extends AbstractOrderMvcTest {

    private static final String READ = "ORDER:READ";
    private static final String WRITE = "ORDER:WRITE";
    private static final String URL = "/api/v1/orders";
    private static final OrderDto ORDER = new OrderDto(1L, 2L, 3L, 3, new BigDecimal("59.97"));

    private static String body(String customerId, String productId, String quantity) {
        return "{\"customerId\":" + customerId + ",\"productId\":" + productId + ",\"quantity\":" + quantity + "}";
    }

    // ---------------------------------------------------------------- GET /orders

    @Test
    void _01_ShouldReturn200WithPageAndDefaults_WhenTokenHoldsOrderRead() throws Exception {
        when(orderService.list(0, 20, null, null)).thenReturn(new PageDto<>(List.of(ORDER), 0, 20, 1));

        JsonNode data = assertSuccess(call(getWith(URL, token(READ))), 200).get("data");

        JsonNode first = data.get("content").get(0);
        assertThat(first.get("id").asLong()).isEqualTo(1L);
        assertThat(first.get("customerId").asLong()).isEqualTo(2L);
        assertThat(first.get("productId").asLong()).isEqualTo(3L);
        assertThat(first.get("quantity").asInt()).isEqualTo(3);
        assertThat(first.get("total").decimalValue()).isEqualByComparingTo("59.97");
        assertThat(data.get("page").asInt()).isZero();
        assertThat(data.get("size").asInt()).isEqualTo(20);
        assertThat(data.get("totalElements").asLong()).isEqualTo(1L);
        assertThat(data.get("totalPages").asInt()).isEqualTo(1);
    }

    @Test
    void _02_ShouldPassPageSizeAndBothFiltersToTheService_WhenTheyAreGiven() throws Exception {
        when(orderService.list(2, 5, 7L, 8L)).thenReturn(new PageDto<>(List.of(), 2, 5, 11));

        JsonNode data = assertSuccess(call(getWith(URL + "?page=2&size=5&customerId=7&productId=8", token(READ))), 200)
                .get("data");

        assertThat(data.get("page").asInt()).isEqualTo(2);
        verify(orderService).list(2, 5, 7L, 8L);
    }

    @Test
    void _03_ShouldPassOnlyTheCustomerFilter_WhenProductIdIsAbsent() throws Exception {
        when(orderService.list(0, 20, 7L, null)).thenReturn(new PageDto<>(List.of(ORDER), 0, 20, 1));

        assertSuccess(call(getWith(URL + "?customerId=7", token(READ))), 200);

        verify(orderService).list(0, 20, 7L, null);
    }

    @Test
    void _04_ShouldPassOnlyTheProductFilter_WhenCustomerIdIsAbsent() throws Exception {
        when(orderService.list(0, 20, null, 8L)).thenReturn(new PageDto<>(List.of(ORDER), 0, 20, 1));

        assertSuccess(call(getWith(URL + "?productId=8", token(READ))), 200);

        verify(orderService).list(0, 20, null, 8L);
    }

    @Test
    void _05_ShouldReturn400_WhenAFilterIsNotANumber() throws Exception {
        assertError(call(getWith(URL + "?customerId=abc", token(READ))), 400);
        assertError(call(getWith(URL + "?productId=abc", token(READ))), 400);
        verifyNoInteractions(orderService);
    }

    @Test
    void _06_ShouldKeepEmptyContentInTheJson_WhenPageIsBeyondTheLastOne() throws Exception {
        when(orderService.list(99, 10, null, null)).thenReturn(new PageDto<>(List.of(), 99, 10, 3));

        JsonNode data = assertSuccess(call(getWith(URL + "?page=99&size=10", token(READ))), 200).get("data");

        assertThat(data.has("content")).isTrue();
        assertThat(data.get("content").isArray()).isTrue();
        assertThat(data.get("content")).isEmpty();
        assertThat(data.get("totalElements").asLong()).isEqualTo(3L);
    }

    @Test
    void _07_ShouldReturn400NamingPage_WhenPageIsNegative() throws Exception {
        JsonNode json = assertError(call(getWith(URL + "?page=-1", token(READ))), 400);

        assertThat(json.get("message").asText()).contains("page: ").doesNotContain("size: ");
        verifyNoInteractions(orderService);
    }

    @Test
    void _08_ShouldReturn400NamingSize_WhenSizeIsZeroOrAbove100() throws Exception {
        JsonNode zero = assertError(call(getWith(URL + "?size=0", token(READ))), 400);
        JsonNode big = assertError(call(getWith(URL + "?size=101", token(READ))), 400);

        assertThat(zero.get("message").asText()).contains("size: ").doesNotContain("page: ");
        assertThat(big.get("message").asText()).contains("size: ");
        verifyNoInteractions(orderService);
    }

    @Test
    void _09_ShouldAcceptBoundarySizes_WhenSizeIs1Or100() throws Exception {
        when(orderService.list(0, 1, null, null)).thenReturn(new PageDto<>(List.of(), 0, 1, 0));
        when(orderService.list(0, 100, null, null)).thenReturn(new PageDto<>(List.of(), 0, 100, 0));

        assertSuccess(call(getWith(URL + "?size=1", token(READ))), 200);
        assertSuccess(call(getWith(URL + "?size=100", token(READ))), 200);
    }

    // ---------------------------------------------------------------- GET /orders/{id}

    @Test
    void _10_ShouldReturn200WithTheOrder_WhenOrderExists() throws Exception {
        when(orderService.get(1L)).thenReturn(ORDER);

        JsonNode json = assertSuccess(call(getWith(URL + "/1", token(READ))), 200);

        assertThat(json.get("data").get("id").asLong()).isEqualTo(1L);
        assertThat(json.get("data").get("total").decimalValue()).isEqualByComparingTo("59.97");
        assertThat(json.get("message").asText()).isEqualTo("Order");
    }

    @Test
    void _11_ShouldReturn404ApiResponse_WhenOrderDoesNotExist() throws Exception {
        when(orderService.get(99L)).thenThrow(new ResourceNotFoundException("Order not found: 99"));

        JsonNode json = assertError(call(getWith(URL + "/99", token(READ))), 404);

        assertThat(json.get("message").asText()).isEqualTo("Order not found: 99");
    }

    @Test
    void _12_ShouldReturn400_WhenOrderIdIsNotANumber() throws Exception {
        assertError(call(getWith(URL + "/abc", token(READ))), 400);
        verifyNoInteractions(orderService);
    }

    // ---------------------------------------------------------------- POST /orders

    @Test
    void _13_ShouldReturn201WithCreatedOrder_WhenBodyIsValid() throws Exception {
        when(orderService.create(any(OrderDto.class))).thenReturn(ORDER);

        JsonNode json = assertSuccess(call(postJson(URL, token(WRITE), body("2", "3", "3"))), 201);

        assertThat(json.get("data").get("id").asLong()).isEqualTo(1L);
        assertThat(json.get("data").get("total").decimalValue()).isEqualByComparingTo("59.97");
        assertThat(json.get("message").asText()).isEqualTo("Order created");
        ArgumentCaptor<OrderDto> sent = ArgumentCaptor.forClass(OrderDto.class);
        verify(orderService).create(sent.capture());
        assertThat(sent.getValue()).isEqualTo(new OrderDto(null, 2L, 3L, 3, null));
    }

    @Test
    void _14_ShouldIgnoreAClientSuppliedTotal_WhenBodyCarriesOne() throws Exception {
        when(orderService.create(any(OrderDto.class))).thenReturn(ORDER);

        assertSuccess(call(postJson(URL, token(WRITE),
                "{\"customerId\":2,\"productId\":3,\"quantity\":3,\"total\":0.01,\"id\":77}")), 201);

        ArgumentCaptor<OrderDto> sent = ArgumentCaptor.forClass(OrderDto.class);
        verify(orderService).create(sent.capture());
        assertThat(sent.getValue().getTotal()).isNull();
        assertThat(sent.getValue().getId()).isNull();
    }

    @Test
    void _15_ShouldReturn400NamingTheField_WhenCustomerIdOrProductIdIsNull() throws Exception {
        JsonNode customer = assertError(call(postJson(URL, token(WRITE), body("null", "3", "1"))), 400);
        JsonNode product = assertError(call(postJson(URL, token(WRITE), body("2", "null", "1"))), 400);
        JsonNode missing = assertError(call(postJson(URL, token(WRITE), "{\"quantity\":1}")), 400);

        assertThat(customer.get("message").asText()).contains("customerId: ").doesNotContain("productId: ");
        assertThat(product.get("message").asText()).contains("productId: ").doesNotContain("customerId: ");
        assertThat(missing.get("message").asText()).contains("customerId: ").contains("productId: ");
        verifyNoInteractions(orderService);
    }

    @Test
    void _16_ShouldReturn400NamingQuantity_WhenQuantityIsMissingZeroOrTooLarge() throws Exception {
        JsonNode missing = assertError(call(postJson(URL, token(WRITE), "{\"customerId\":2,\"productId\":3}")), 400);
        JsonNode zero = assertError(call(postJson(URL, token(WRITE), body("2", "3", "0"))), 400);
        JsonNode negative = assertError(call(postJson(URL, token(WRITE), body("2", "3", "-1"))), 400);
        JsonNode big = assertError(call(postJson(URL, token(WRITE), body("2", "3", "1000001"))), 400);

        for (JsonNode json : List.of(missing, zero, negative, big)) {
            assertThat(json.get("message").asText()).contains("quantity: ");
        }
        verifyNoInteractions(orderService);
    }

    @Test
    void _17_ShouldReturn201_WhenQuantityIsOnItsLimits() throws Exception {
        when(orderService.create(any(OrderDto.class))).thenReturn(ORDER);

        assertSuccess(call(postJson(URL, token(WRITE), body("2", "3", "1"))), 201);
        assertSuccess(call(postJson(URL, token(WRITE), body("2", "3", "1000000"))), 201);
    }

    @Test
    void _18_ShouldReturn400_WhenBodyIsMalformedJsonOrHasTheWrongTypes() throws Exception {
        assertError(call(postJson(URL, token(WRITE), "{\"customerId\":")), 400);
        assertError(call(postJson(URL, token(WRITE), body("\"abc\"", "3", "1"))), 400);
        verifyNoInteractions(orderService);
    }

    @Test
    void _19_ShouldReturn404_WhenTheServiceReportsAnUnknownCustomer() throws Exception {
        when(orderService.create(any(OrderDto.class))).thenThrow(new ResourceNotFoundException("Customer not found: 2"));

        JsonNode json = assertError(call(postJson(URL, token(WRITE), body("2", "3", "1"))), 404);

        assertThat(json.get("message").asText()).isEqualTo("Customer not found: 2");
    }

    @Test
    void _20_ShouldReturn404_WhenTheServiceReportsAnUnknownProduct() throws Exception {
        when(orderService.create(any(OrderDto.class))).thenThrow(new ResourceNotFoundException("Product not found: 3"));

        JsonNode json = assertError(call(postJson(URL, token(WRITE), body("2", "3", "1"))), 404);

        assertThat(json.get("message").asText()).isEqualTo("Product not found: 3");
    }

    @Test
    void _21_ShouldReturn422_WhenTheServiceRefusesTheTotal() throws Exception {
        when(orderService.create(any(OrderDto.class)))
                .thenThrow(new BusinessRuleException("Order total must not exceed 999999999999.99"));

        JsonNode json = assertError(call(postJson(URL, token(WRITE), body("2", "3", "1000000"))), 422);

        assertThat(json.get("message").asText()).isEqualTo("Order total must not exceed 999999999999.99");
    }

    @Test
    void _22_ShouldReturn415_WhenContentTypeIsNotJson() throws Exception {
        assertError(call(MockMvcRequestBuilders.post(URL)
                .header("Authorization", bearer(token(WRITE))).contentType(MediaType.TEXT_PLAIN).content("x")), 415);
        verifyNoInteractions(orderService);
    }

    // ---------------------------------------------------------------- orders are immutable

    @Test
    void _23_ShouldReturn405ApiResponse_WhenPutDeleteOrPatchIsUsed() throws Exception {
        assertError(call(putJson(URL + "/1", token(WRITE), body("2", "3", "1"))), 405);
        assertError(call(deleteWith(URL + "/1", token(WRITE))), 405);
        assertError(call(patchWith(URL + "/1", token(WRITE))), 405);
        assertError(call(deleteWith(URL, token(WRITE))), 405);
        verifyNoInteractions(orderService);
    }
}
