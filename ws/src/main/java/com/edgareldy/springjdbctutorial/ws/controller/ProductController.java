package com.edgareldy.springjdbctutorial.ws.controller;

import com.edgareldy.springjdbctutorial.core.catalog.service.ProductService;
import com.edgareldy.springjdbctutorial.ws.converter.ProductConverter;
import com.edgareldy.springjdbctutorial.ws.payload.catalog.ProductRequest;
import com.edgareldy.springjdbctutorial.ws.payload.catalog.ProductResponse;
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
 * HTTP entry points to manage products. No business logic.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("hasPermission('PRODUCT','READ')")
    public ApiResponse<PageResponse<ProductResponse>> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                           @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                           @RequestParam(required = false) Long categoryId) {
        return ApiResponse.success(ProductConverter.toPageResponse(productService.list(page, size, categoryId)), "Products");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('PRODUCT','READ')")
    public ApiResponse<ProductResponse> get(@PathVariable Long id) {
        return ApiResponse.success(ProductConverter.toResponse(productService.get(id)), "Product");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('PRODUCT','WRITE')")
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(ProductConverter.toResponse(productService.create(ProductConverter.toDto(request))),
                "Product created");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('PRODUCT','WRITE')")
    public ApiResponse<ProductResponse> update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(ProductConverter.toResponse(productService.update(id, ProductConverter.toDto(request))),
                "Product updated");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('PRODUCT','WRITE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.success(null, "Product deleted");
    }
}
