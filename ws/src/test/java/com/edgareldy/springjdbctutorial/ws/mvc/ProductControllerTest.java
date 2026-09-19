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

import com.edgareldy.springjdbctutorial.core.catalog.dto.ProductDto;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * MockMvc tests of ProductController with ProductService mocked but the real security and validation: statuses,
 * ApiResponse shape, page/size bounds, the categoryId filter, per-field body validation (price rules included)
 * and the 404/422 refusals.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
class ProductControllerTest extends AbstractCatalogMvcTest {

    private static final String READ = "PRODUCT:READ";
    private static final String WRITE = "PRODUCT:WRITE";
    private static final String VALID_BODY = "{\"categoryId\":2,\"productName\":\"Chess\",\"unitPrice\":25.99}";

    private static ProductDto chess() {
        return new ProductDto(3L, 2L, "Chess", new BigDecimal("25.99"));
    }

    private static String body(String categoryId, String name, String price) {
        return "{\"categoryId\":" + categoryId + ",\"productName\":" + name + ",\"unitPrice\":" + price + "}";
    }

    // ---------------------------------------------------------------- GET /products

    @Test
    void _01_ShouldReturn200WithPageAndDefaults_WhenTokenHoldsProductRead() throws Exception {
        when(productService.list(0, 20, null)).thenReturn(new PageDto<>(List.of(chess()), 0, 20, 1));

        JsonNode data = assertSuccess(call(getWith("/api/v1/products", token(READ))), 200).get("data");

        JsonNode first = data.get("content").get(0);
        assertThat(first.get("id").asLong()).isEqualTo(3L);
        assertThat(first.get("categoryId").asLong()).isEqualTo(2L);
        assertThat(first.get("productName").asText()).isEqualTo("Chess");
        assertThat(first.get("unitPrice").decimalValue()).isEqualByComparingTo("25.99");
        assertThat(data.get("page").asInt()).isZero();
        assertThat(data.get("size").asInt()).isEqualTo(20);
        assertThat(data.get("totalElements").asLong()).isEqualTo(1L);
    }

    @Test
    void _02_ShouldPassTheCategoryIdFilterToTheService_WhenItIsGiven() throws Exception {
        when(productService.list(1, 5, 2L)).thenReturn(new PageDto<>(List.of(chess()), 1, 5, 6));

        assertSuccess(call(getWith("/api/v1/products?page=1&size=5&categoryId=2", token(READ))), 200);

        verify(productService).list(1, 5, 2L);
    }

    @Test
    void _03_ShouldPassNullFilter_WhenCategoryIdIsAbsent() throws Exception {
        when(productService.list(0, 20, null)).thenReturn(new PageDto<>(List.of(), 0, 20, 0));

        assertSuccess(call(getWith("/api/v1/products", token(READ))), 200);

        verify(productService).list(0, 20, null);
    }

    @Test
    void _04_ShouldReturn400_WhenCategoryIdIsNotANumber() throws Exception {
        JsonNode json = assertError(call(getWith("/api/v1/products?categoryId=abc", token(READ))), 400);

        assertThat(json.get("message").asText()).isEqualTo("Invalid value for parameter: categoryId");
        verifyNoInteractions(productService);
    }

    @Test
    void _05_ShouldKeepEmptyContentInTheJson_WhenPageIsBeyondTheLastOne() throws Exception {
        when(productService.list(99, 10, null)).thenReturn(new PageDto<>(List.of(), 99, 10, 4));

        JsonNode data = assertSuccess(call(getWith("/api/v1/products?page=99&size=10", token(READ))), 200).get("data");

        assertThat(data.get("content").isArray()).isTrue();
        assertThat(data.get("content")).isEmpty();
        assertThat(data.get("totalElements").asLong()).isEqualTo(4L);
    }

    @Test
    void _06_ShouldReturn400NamingPage_WhenPageIsNegative() throws Exception {
        JsonNode json = assertError(call(getWith("/api/v1/products?page=-1", token(READ))), 400);

        assertThat(json.get("message").asText()).contains("page: ").doesNotContain("size: ");
        verifyNoInteractions(productService);
    }

    @Test
    void _07_ShouldReturn400NamingSize_WhenSizeIsZeroOrAbove100() throws Exception {
        JsonNode zero = assertError(call(getWith("/api/v1/products?size=0", token(READ))), 400);
        JsonNode big = assertError(call(getWith("/api/v1/products?size=101", token(READ))), 400);

        assertThat(zero.get("message").asText()).contains("size: ").doesNotContain("page: ");
        assertThat(big.get("message").asText()).contains("size: ");
        verifyNoInteractions(productService);
    }

    @Test
    void _08_ShouldAcceptBoundarySizes_WhenSizeIs1Or100() throws Exception {
        when(productService.list(0, 1, null)).thenReturn(new PageDto<>(List.of(), 0, 1, 0));
        when(productService.list(0, 100, null)).thenReturn(new PageDto<>(List.of(), 0, 100, 0));

        assertSuccess(call(getWith("/api/v1/products?size=1", token(READ))), 200);
        assertSuccess(call(getWith("/api/v1/products?size=100", token(READ))), 200);
    }

    // ---------------------------------------------------------------- GET /products/{id}

    @Test
    void _09_ShouldReturn200_WhenProductExists() throws Exception {
        when(productService.get(3L)).thenReturn(chess());

        JsonNode json = assertSuccess(call(getWith("/api/v1/products/3", token(READ))), 200);

        assertThat(json.get("data").get("productName").asText()).isEqualTo("Chess");
    }

    @Test
    void _10_ShouldReturn404ApiResponse_WhenProductDoesNotExist() throws Exception {
        when(productService.get(99L)).thenThrow(new ResourceNotFoundException("Product not found: 99"));

        JsonNode json = assertError(call(getWith("/api/v1/products/99", token(READ))), 404);

        assertThat(json.get("message").asText()).isEqualTo("Product not found: 99");
    }

    // ---------------------------------------------------------------- POST /products

    @Test
    void _11_ShouldReturn201WithCreatedProduct_WhenBodyIsValid() throws Exception {
        when(productService.create(any(ProductDto.class))).thenReturn(chess());

        JsonNode json = assertSuccess(call(postJson("/api/v1/products", token(WRITE), VALID_BODY)), 201);

        assertThat(json.get("data").get("id").asLong()).isEqualTo(3L);
        assertThat(json.get("message").asText()).isEqualTo("Product created");
        ArgumentCaptor<ProductDto> sent = ArgumentCaptor.forClass(ProductDto.class);
        verify(productService).create(sent.capture());
        assertThat(sent.getValue().getCategoryId()).isEqualTo(2L);
        assertThat(sent.getValue().getProductName()).isEqualTo("Chess");
        assertThat(sent.getValue().getUnitPrice()).isEqualByComparingTo("25.99");
    }

    @Test
    void _12_ShouldReturn400NamingEveryField_WhenBodyIsEmpty() throws Exception {
        JsonNode json = assertError(call(postJson("/api/v1/products", token(WRITE), "{}")), 400);

        assertThat(json.get("message").asText()).contains("categoryId: ").contains("productName: ")
                .contains("unitPrice: ");
        verifyNoInteractions(productService);
    }

    @Test
    void _13_ShouldReturn400NamingOnlyProductName_WhenNameIsBlankOrTooLong() throws Exception {
        JsonNode blank = assertError(call(postJson("/api/v1/products", token(WRITE), body("2", "\" \"", "1.00"))), 400);
        JsonNode tooLong = assertError(call(postJson("/api/v1/products", token(WRITE),
                body("2", "\"" + "x".repeat(151) + "\"", "1.00"))), 400);

        assertThat(blank.get("message").asText()).contains("productName: ").doesNotContain("unitPrice: ");
        assertThat(tooLong.get("message").asText()).contains("productName: ").doesNotContain("unitPrice: ");
        verifyNoInteractions(productService);
    }

    @Test
    void _14_ShouldReturn400NamingUnitPrice_WhenPriceIsNegative() throws Exception {
        JsonNode json = assertError(call(postJson("/api/v1/products", token(WRITE), body("2", "\"Chess\"", "-0.01"))), 400);

        assertThat(json.get("message").asText()).contains("unitPrice: ").doesNotContain("productName: ");
        verifyNoInteractions(productService);
    }

    @Test
    void _15_ShouldReturn400NamingUnitPrice_WhenPriceHasThreeDecimals() throws Exception {
        JsonNode json = assertError(call(postJson("/api/v1/products", token(WRITE), body("2", "\"Chess\"", "1.005"))), 400);

        assertThat(json.get("message").asText()).contains("unitPrice: ");
        verifyNoInteractions(productService);
    }

    @Test
    void _16_ShouldReturn400NamingUnitPrice_WhenPriceHasMoreThanTenIntegerDigits() throws Exception {
        JsonNode json = assertError(call(postJson("/api/v1/products", token(WRITE),
                body("2", "\"Chess\"", "10000000000.00"))), 400);

        assertThat(json.get("message").asText()).contains("unitPrice: ");
        verifyNoInteractions(productService);
    }

    @Test
    void _17_ShouldAcceptTheMaximumPriceAndZero_WhenOnTheBoundaries() throws Exception {
        when(productService.create(any(ProductDto.class))).thenReturn(chess());

        assertSuccess(call(postJson("/api/v1/products", token(WRITE), body("2", "\"Gold\"", "9999999999.99"))), 201);
        assertSuccess(call(postJson("/api/v1/products", token(WRITE), body("2", "\"Free\"", "0"))), 201);
    }

    @Test
    void _18_ShouldReturn400_WhenPriceIsNotANumber() throws Exception {
        assertError(call(postJson("/api/v1/products", token(WRITE), body("2", "\"Chess\"", "\"abc\""))), 400);
        verifyNoInteractions(productService);
    }

    @Test
    void _19_ShouldReturn404_WhenServiceReportsAnUnknownCategory() throws Exception {
        when(productService.create(any(ProductDto.class)))
                .thenThrow(new ResourceNotFoundException("Category not found: 2"));

        JsonNode json = assertError(call(postJson("/api/v1/products", token(WRITE), VALID_BODY)), 404);

        assertThat(json.get("message").asText()).isEqualTo("Category not found: 2");
    }

    @Test
    void _20_ShouldReturn415_WhenContentTypeIsNotJson() throws Exception {
        assertError(call(MockMvcRequestBuilders.post("/api/v1/products")
                .header("Authorization", bearer(token(WRITE))).contentType(MediaType.TEXT_PLAIN).content("Chess")), 415);
        verifyNoInteractions(productService);
    }

    // ---------------------------------------------------------------- PUT /products/{id}

    @Test
    void _21_ShouldReturn200WithUpdatedProduct_WhenBodyIsValid() throws Exception {
        when(productService.update(eq(3L), any(ProductDto.class))).thenReturn(chess());

        JsonNode json = assertSuccess(call(putJson("/api/v1/products/3", token(WRITE), VALID_BODY)), 200);

        assertThat(json.get("message").asText()).isEqualTo("Product updated");
        assertThat(json.get("data").get("unitPrice").decimalValue()).isEqualByComparingTo("25.99");
    }

    @Test
    void _22_ShouldReturn404_WhenUpdatingAnUnknownProductOrCategory() throws Exception {
        when(productService.update(eq(99L), any(ProductDto.class)))
                .thenThrow(new ResourceNotFoundException("Product not found: 99"));
        when(productService.update(eq(3L), any(ProductDto.class)))
                .thenThrow(new ResourceNotFoundException("Category not found: 2"));

        assertError(call(putJson("/api/v1/products/99", token(WRITE), VALID_BODY)), 404);
        assertError(call(putJson("/api/v1/products/3", token(WRITE), VALID_BODY)), 404);
    }

    @Test
    void _23_ShouldReturn422_WhenServiceRefusesTheUpdate() throws Exception {
        when(productService.update(eq(3L), any(ProductDto.class)))
                .thenThrow(new BusinessRuleException("Unit price must not be negative"));

        assertError(call(putJson("/api/v1/products/3", token(WRITE), VALID_BODY)), 422);
    }

    @Test
    void _24_ShouldReturn400_WhenUpdatedBodyIsInvalid() throws Exception {
        JsonNode json = assertError(call(putJson("/api/v1/products/3", token(WRITE), body("null", "\"\"", "-1"))), 400);

        assertThat(json.get("message").asText()).contains("categoryId: ").contains("productName: ")
                .contains("unitPrice: ");
        verifyNoInteractions(productService);
    }

    // ---------------------------------------------------------------- DELETE /products/{id}

    @Test
    void _25_ShouldReturn200WithoutData_WhenProductIsDeleted() throws Exception {
        JsonNode json = assertSuccess(call(deleteWith("/api/v1/products/3", token(WRITE))), 200);

        assertThat(json.get("message").asText()).isEqualTo("Product deleted");
        assertThat(json.get("data") == null || json.get("data").isNull()).isTrue();
        verify(productService).delete(3L);
    }

    @Test
    void _26_ShouldReturn404ApiResponse_WhenDeletingAnUnknownProduct() throws Exception {
        doThrow(new ResourceNotFoundException("Product not found: 99")).when(productService).delete(99L);

        assertError(call(deleteWith("/api/v1/products/99", token(WRITE))), 404);
    }

    @Test
    void _27_ShouldReturn422_WhenAnOrderReferencesTheProduct() throws Exception {
        doThrow(new BusinessRuleException("Product is referenced by orders")).when(productService).delete(3L);

        JsonNode json = assertError(call(deleteWith("/api/v1/products/3", token(WRITE))), 422);

        assertThat(json.get("message").asText()).isEqualTo("Product is referenced by orders");
    }

    // ---------------------------------------------------------------- unsupported method

    @Test
    void _28_ShouldReturn405ApiResponse_WhenPatchIsUsedOnAProduct() throws Exception {
        assertError(call(patchWith("/api/v1/products/3", token(WRITE))), 405);
    }
}
