package com.edgareldy.springjdbctutorial.ws.controller;

import com.edgareldy.springjdbctutorial.core.order.service.OrderService;
import com.edgareldy.springjdbctutorial.ws.converter.OrderConverter;
import com.edgareldy.springjdbctutorial.ws.payload.common.ApiResponse;
import com.edgareldy.springjdbctutorial.ws.payload.common.PageResponse;
import com.edgareldy.springjdbctutorial.ws.payload.order.OrderRequest;
import com.edgareldy.springjdbctutorial.ws.payload.order.OrderResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry points to read and place orders. No business logic.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @PreAuthorize("hasPermission('ORDER','READ')")
    public ApiResponse<PageResponse<OrderResponse>> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                         @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
                                                         @RequestParam(required = false) Long customerId,
                                                         @RequestParam(required = false) Long productId) {
        return ApiResponse.success(
                OrderConverter.toPageResponse(orderService.list(page, size, customerId, productId)), "Orders");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('ORDER','READ')")
    public ApiResponse<OrderResponse> get(@PathVariable Long id) {
        return ApiResponse.success(OrderConverter.toResponse(orderService.get(id)), "Order");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasPermission('ORDER','WRITE')")
    public ApiResponse<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        return ApiResponse.success(OrderConverter.toResponse(orderService.create(OrderConverter.toDto(request))),
                "Order created");
    }
}
