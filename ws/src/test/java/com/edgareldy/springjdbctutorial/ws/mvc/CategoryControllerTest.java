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

import com.edgareldy.springjdbctutorial.core.catalog.dto.CategoryDto;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * MockMvc tests of CategoryController with CategoryService mocked but the real security and validation: statuses,
 * ApiResponse shape, page/size bounds, per-field body validation and the 404/422 refusals.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
class CategoryControllerTest extends AbstractCatalogMvcTest {

    private static final String READ = "CATEGORY:READ";
    private static final String WRITE = "CATEGORY:WRITE";
    private static final String VALID_BODY = "{\"categoryName\":\"Books\"}";

    // ---------------------------------------------------------------- GET /categories

    @Test
    void _01_ShouldReturn200WithPageAndDefaults_WhenTokenHoldsCategoryRead() throws Exception {
        when(categoryService.list(0, 20)).thenReturn(new PageDto<>(List.of(new CategoryDto(1L, "Books")), 0, 20, 1));

        JsonNode json = assertSuccess(call(getWith("/api/v1/categories", token(READ))), 200);

        JsonNode data = json.get("data");
        assertThat(data.get("content").get(0).get("id").asLong()).isEqualTo(1L);
        assertThat(data.get("content").get(0).get("categoryName").asText()).isEqualTo("Books");
        assertThat(data.get("page").asInt()).isZero();
        assertThat(data.get("size").asInt()).isEqualTo(20);
        assertThat(data.get("totalElements").asLong()).isEqualTo(1L);
        assertThat(data.get("totalPages").asInt()).isEqualTo(1);
    }

    @Test
    void _02_ShouldPassPageAndSizeToTheService_WhenTheyAreGiven() throws Exception {
        when(categoryService.list(2, 5)).thenReturn(new PageDto<>(List.of(), 2, 5, 11));

        JsonNode json = assertSuccess(call(getWith("/api/v1/categories?page=2&size=5", token(READ))), 200);

        assertThat(json.get("data").get("page").asInt()).isEqualTo(2);
        assertThat(json.get("data").get("size").asInt()).isEqualTo(5);
        verify(categoryService).list(2, 5);
    }

    @Test
    void _03_ShouldKeepEmptyContentInTheJson_WhenPageIsBeyondTheLastOne() throws Exception {
        when(categoryService.list(99, 10)).thenReturn(new PageDto<>(List.of(), 99, 10, 3));

        JsonNode data = assertSuccess(call(getWith("/api/v1/categories?page=99&size=10", token(READ))), 200).get("data");

        assertThat(data.has("content")).isTrue();
        assertThat(data.get("content").isArray()).isTrue();
        assertThat(data.get("content")).isEmpty();
        assertThat(data.get("totalElements").asLong()).isEqualTo(3L);
    }

    @Test
    void _04_ShouldReturn400NamingPage_WhenPageIsNegative() throws Exception {
        JsonNode json = assertError(call(getWith("/api/v1/categories?page=-1", token(READ))), 400);

        assertThat(json.get("message").asText()).contains("page: ").doesNotContain("size: ");
        verifyNoInteractions(categoryService);
    }

    @Test
    void _05_ShouldReturn400NamingSize_WhenSizeIsZeroOrAbove100() throws Exception {
        JsonNode zero = assertError(call(getWith("/api/v1/categories?size=0", token(READ))), 400);
        JsonNode big = assertError(call(getWith("/api/v1/categories?size=101", token(READ))), 400);

        assertThat(zero.get("message").asText()).contains("size: ").doesNotContain("page: ");
        assertThat(big.get("message").asText()).contains("size: ");
        verifyNoInteractions(categoryService);
    }

    @Test
    void _06_ShouldReturn400NamingBoth_WhenPageAndSizeAreInvalid() throws Exception {
        JsonNode json = assertError(call(getWith("/api/v1/categories?page=-5&size=500", token(READ))), 400);

        assertThat(json.get("message").asText()).contains("page: ").contains("size: ");
    }

    @Test
    void _07_ShouldAcceptBoundarySizes_WhenSizeIs1Or100() throws Exception {
        when(categoryService.list(0, 1)).thenReturn(new PageDto<>(List.of(), 0, 1, 0));
        when(categoryService.list(0, 100)).thenReturn(new PageDto<>(List.of(), 0, 100, 0));

        assertSuccess(call(getWith("/api/v1/categories?size=1", token(READ))), 200);
        assertSuccess(call(getWith("/api/v1/categories?size=100", token(READ))), 200);
    }

    // ---------------------------------------------------------------- GET /categories/{id}

    @Test
    void _08_ShouldReturn200_WhenCategoryExists() throws Exception {
        when(categoryService.get(2L)).thenReturn(new CategoryDto(2L, "Games"));

        JsonNode json = assertSuccess(call(getWith("/api/v1/categories/2", token(READ))), 200);

        assertThat(json.get("data").get("id").asLong()).isEqualTo(2L);
        assertThat(json.get("data").get("categoryName").asText()).isEqualTo("Games");
    }

    @Test
    void _09_ShouldReturn404ApiResponse_WhenCategoryDoesNotExist() throws Exception {
        when(categoryService.get(99L)).thenThrow(new ResourceNotFoundException("Category not found: 99"));

        JsonNode json = assertError(call(getWith("/api/v1/categories/99", token(READ))), 404);

        assertThat(json.get("message").asText()).isEqualTo("Category not found: 99");
    }

    @Test
    void _10_ShouldReturn400_WhenCategoryIdIsNotANumber() throws Exception {
        assertError(call(getWith("/api/v1/categories/abc", token(READ))), 400);
        verifyNoInteractions(categoryService);
    }

    // ---------------------------------------------------------------- POST /categories

    @Test
    void _11_ShouldReturn201WithCreatedCategory_WhenBodyIsValid() throws Exception {
        when(categoryService.create(any(CategoryDto.class))).thenReturn(new CategoryDto(7L, "Books"));

        JsonNode json = assertSuccess(call(postJson("/api/v1/categories", token(WRITE), VALID_BODY)), 201);

        assertThat(json.get("data").get("id").asLong()).isEqualTo(7L);
        assertThat(json.get("message").asText()).isEqualTo("Category created");
        ArgumentCaptor<CategoryDto> sent = ArgumentCaptor.forClass(CategoryDto.class);
        verify(categoryService).create(sent.capture());
        assertThat(sent.getValue().getCategoryName()).isEqualTo("Books");
    }

    @Test
    void _12_ShouldReturn400NamingTheField_WhenNameIsBlank() throws Exception {
        JsonNode json = assertError(call(postJson("/api/v1/categories", token(WRITE), "{\"categoryName\":\"  \"}")), 400);

        assertThat(json.get("message").asText()).contains("categoryName: ");
        verifyNoInteractions(categoryService);
    }

    @Test
    void _13_ShouldReturn400NamingTheField_WhenNameIsMissingOrTooLong() throws Exception {
        JsonNode missing = assertError(call(postJson("/api/v1/categories", token(WRITE), "{}")), 400);
        JsonNode tooLong = assertError(call(postJson("/api/v1/categories", token(WRITE),
                "{\"categoryName\":\"" + "x".repeat(101) + "\"}")), 400);

        assertThat(missing.get("message").asText()).contains("categoryName: ");
        assertThat(tooLong.get("message").asText()).contains("categoryName: ");
        verifyNoInteractions(categoryService);
    }

    @Test
    void _14_ShouldReturn400_WhenBodyIsMalformedJson() throws Exception {
        assertError(call(postJson("/api/v1/categories", token(WRITE), "{\"categoryName\":")), 400);
        verifyNoInteractions(categoryService);
    }

    @Test
    void _15_ShouldReturn422_WhenServiceRefusesADuplicateName() throws Exception {
        when(categoryService.create(any(CategoryDto.class)))
                .thenThrow(new BusinessRuleException("Category name already exists"));

        JsonNode json = assertError(call(postJson("/api/v1/categories", token(WRITE), VALID_BODY)), 422);

        assertThat(json.get("message").asText()).isEqualTo("Category name already exists");
    }

    @Test
    void _16_ShouldReturn415_WhenContentTypeIsNotJson() throws Exception {
        assertError(call(MockMvcRequestBuilders.post("/api/v1/categories")
                .header("Authorization", bearer(token(WRITE))).contentType(MediaType.TEXT_PLAIN).content("Books")), 415);
        verifyNoInteractions(categoryService);
    }

    // ---------------------------------------------------------------- PUT /categories/{id}

    @Test
    void _17_ShouldReturn200WithUpdatedCategory_WhenBodyIsValid() throws Exception {
        when(categoryService.update(eq(2L), any(CategoryDto.class))).thenReturn(new CategoryDto(2L, "Books"));

        JsonNode json = assertSuccess(call(putJson("/api/v1/categories/2", token(WRITE), VALID_BODY)), 200);

        assertThat(json.get("data").get("categoryName").asText()).isEqualTo("Books");
        assertThat(json.get("message").asText()).isEqualTo("Category updated");
    }

    @Test
    void _18_ShouldReturn404ApiResponse_WhenUpdatingAnUnknownCategory() throws Exception {
        when(categoryService.update(eq(99L), any(CategoryDto.class)))
                .thenThrow(new ResourceNotFoundException("Category not found: 99"));

        assertError(call(putJson("/api/v1/categories/99", token(WRITE), VALID_BODY)), 404);
    }

    @Test
    void _19_ShouldReturn422_WhenRenamingToAnExistingName() throws Exception {
        when(categoryService.update(eq(2L), any(CategoryDto.class)))
                .thenThrow(new BusinessRuleException("Category name already exists"));

        assertError(call(putJson("/api/v1/categories/2", token(WRITE), VALID_BODY)), 422);
    }

    @Test
    void _20_ShouldReturn400_WhenUpdatedNameIsBlank() throws Exception {
        JsonNode json = assertError(call(putJson("/api/v1/categories/2", token(WRITE), "{\"categoryName\":\"\"}")), 400);

        assertThat(json.get("message").asText()).contains("categoryName: ");
        verifyNoInteractions(categoryService);
    }

    // ---------------------------------------------------------------- DELETE /categories/{id}

    @Test
    void _21_ShouldReturn200WithoutData_WhenCategoryIsDeleted() throws Exception {
        JsonNode json = assertSuccess(call(deleteWith("/api/v1/categories/2", token(WRITE))), 200);

        assertThat(json.get("message").asText()).isEqualTo("Category deleted");
        assertThat(json.get("data") == null || json.get("data").isNull()).isTrue();
        verify(categoryService).delete(2L);
    }

    @Test
    void _22_ShouldReturn404ApiResponse_WhenDeletingAnUnknownCategory() throws Exception {
        doThrow(new ResourceNotFoundException("Category not found: 99"))
                .when(categoryService).delete(99L);

        assertError(call(deleteWith("/api/v1/categories/99", token(WRITE))), 404);
    }

    @Test
    void _23_ShouldReturn422_WhenCategoryStillHasProducts() throws Exception {
        doThrow(new BusinessRuleException("Category still has products"))
                .when(categoryService).delete(2L);

        JsonNode json = assertError(call(deleteWith("/api/v1/categories/2", token(WRITE))), 422);

        assertThat(json.get("message").asText()).isEqualTo("Category still has products");
    }

    // ---------------------------------------------------------------- unsupported method

    @Test
    void _24_ShouldReturn405ApiResponse_WhenPatchIsUsedOnACategory() throws Exception {
        assertError(call(patchWith("/api/v1/categories/2", token(WRITE))), 405);
    }
}
