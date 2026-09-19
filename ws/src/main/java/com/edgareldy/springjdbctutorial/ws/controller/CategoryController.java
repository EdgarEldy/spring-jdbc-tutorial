package com.edgareldy.springjdbctutorial.ws.controller;

import com.edgareldy.springjdbctutorial.core.catalog.service.CategoryService;
import com.edgareldy.springjdbctutorial.ws.converter.CategoryConverter;
import com.edgareldy.springjdbctutorial.ws.payload.catalog.CategoryRequest;
import com.edgareldy.springjdbctutorial.ws.payload.catalog.CategoryResponse;
import com.edgareldy.springjdbctutorial.ws.payload.common.ApiResponse;
import com.edgareldy.springjdbctutorial.ws.payload.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry points to manage categories. No business logic.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("hasPermission('CATEGORY','READ')")
    public ApiResponse<PageResponse<CategoryResponse>> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(CategoryConverter.toPageResponse(categoryService.list(page, size)), "Categories");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('CATEGORY','READ')")
    public ApiResponse<CategoryResponse> get(@PathVariable Long id) {
        return ApiResponse.success(CategoryConverter.toResponse(categoryService.get(id)), "Category");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('CATEGORY','WRITE')")
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(CategoryConverter.toResponse(categoryService.create(CategoryConverter.toDto(request))),
                "Category created");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('CATEGORY','WRITE')")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(CategoryConverter.toResponse(categoryService.update(id, CategoryConverter.toDto(request))),
                "Category updated");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('CATEGORY','WRITE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success(null, "Category deleted");
    }
}
