package com.edgareldy.springjdbctutorial.core.order.service.impl;

import com.edgareldy.springjdbctutorial.core.catalog.dto.ProductDto;
import com.edgareldy.springjdbctutorial.core.catalog.service.ProductService;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import com.edgareldy.springjdbctutorial.core.customer.service.CustomerService;
import com.edgareldy.springjdbctutorial.core.order.dao.OrderDao;
import com.edgareldy.springjdbctutorial.core.order.dto.OrderDto;
import com.edgareldy.springjdbctutorial.core.order.entity.Order;
import com.edgareldy.springjdbctutorial.core.order.mapper.OrderMapper;
import com.edgareldy.springjdbctutorial.core.order.service.OrderService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of OrderService. Declared by @Bean in ServiceConfig (no stereotype) and returned as the interface so the @Transactional advice applies through the proxy.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class OrderServiceImpl implements OrderService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 1_000_000;
    /** Largest value of the NUMERIC(14,2) total column. */
    private static final BigDecimal MAX_TOTAL = new BigDecimal("999999999999.99");

    private final OrderDao orderDao;
    private final ProductService productService;
    private final CustomerService customerService;
    private final OrderMapper mapper = new OrderMapper();

    public OrderServiceImpl(OrderDao orderDao, ProductService productService, CustomerService customerService) {
        this.orderDao = orderDao;
        this.productService = productService;
        this.customerService = customerService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<OrderDto> list(int page, int size, Long customerId, Long productId) {
        if (page < 0) {
            throw new BusinessRuleException("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessRuleException("Size must be between 1 and " + MAX_PAGE_SIZE);
        }
        List<OrderDto> content = new ArrayList<>();
        for (Order order : orderDao.findPage(page, size, customerId, productId)) {
            content.add(mapper.toDto(order));
        }
        return new PageDto<>(content, page, size, orderDao.count(customerId, productId));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto get(Long id) {
        return mapper.toDto(orderDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id)));
    }

    @Override
    @Transactional
    public OrderDto create(OrderDto input) {
        if (input.getCustomerId() == null) {
            throw new BusinessRuleException("Customer id must not be null");
        }
        if (input.getProductId() == null) {
            throw new BusinessRuleException("Product id must not be null");
        }
        int quantity = input.getQuantity();
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new BusinessRuleException("Quantity must be between " + MIN_QUANTITY + " and " + MAX_QUANTITY);
        }
        // Cross-module dependency: the order module reaches customers and products only through their
        // SERVICE interfaces (never their DAOs). The beans are the transactional proxies, so these calls
        // join this transaction, and their ResourceNotFoundException propagates as a 404.
        customerService.get(input.getCustomerId());
        ProductDto product = productService.get(input.getProductId());

        // The exact product is compared with the column limit before any scaling
        BigDecimal exact = product.getUnitPrice().multiply(BigDecimal.valueOf(quantity));
        if (exact.compareTo(MAX_TOTAL) > 0) {
            throw new BusinessRuleException("Order total must not exceed " + MAX_TOTAL.toPlainString());
        }
        // MAX_TOTAL has two decimals, so rounding a value at or below it to two decimals cannot exceed it
        BigDecimal total = exact.setScale(2, RoundingMode.HALF_UP);

        Order order = new Order(null, input.getCustomerId(), input.getProductId(), quantity, total);
        try {
            return mapper.toDto(orderDao.insert(order));
        } catch (DataIntegrityViolationException e) {
            // The customer or the product was deleted between the checks and the insert (foreign key)
            throw new ResourceNotFoundException("Customer or product no longer exists");
        }
    }
}
