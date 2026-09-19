package com.edgareldy.springjdbctutorial.core.order.service;

import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.order.dto.OrderDto;

/**
 * Business operations on orders. Dto types only at this boundary. Orders are immutable.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public interface OrderService {

    /** One page of orders, optionally restricted to a customer and/or a product (filters may be null). */
    PageDto<OrderDto> list(int page, int size, Long customerId, Long productId);

    /** Throws ResourceNotFoundException when the id is unknown. */
    OrderDto get(Long id);

    /** The customer and the product must exist. The total is computed as unit price times quantity. */
    OrderDto create(OrderDto input);
}
