package com.edgareldy.springjdbctutorial.ws.controller;

import com.edgareldy.springjdbctutorial.core.customer.service.CustomerService;
import com.edgareldy.springjdbctutorial.ws.converter.CustomerConverter;
import com.edgareldy.springjdbctutorial.ws.payload.customer.CustomerRequest;
import com.edgareldy.springjdbctutorial.ws.payload.customer.CustomerResponse;
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
 * HTTP entry points to manage customers. No business logic.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @PreAuthorize("hasPermission('CUSTOMER','READ')")
    public ApiResponse<PageResponse<CustomerResponse>> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(CustomerConverter.toPageResponse(customerService.list(page, size)), "Customers");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('CUSTOMER','READ')")
    public ApiResponse<CustomerResponse> get(@PathVariable Long id) {
        return ApiResponse.success(CustomerConverter.toResponse(customerService.get(id)), "Customer");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('CUSTOMER','WRITE')")
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ApiResponse.success(CustomerConverter.toResponse(customerService.create(CustomerConverter.toDto(request))),
                "Customer created");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasPermission('CUSTOMER','WRITE')")
    public ApiResponse<CustomerResponse> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return ApiResponse.success(CustomerConverter.toResponse(customerService.update(id, CustomerConverter.toDto(request))),
                "Customer updated");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission('CUSTOMER','WRITE')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.success(null, "Customer deleted");
    }
}
