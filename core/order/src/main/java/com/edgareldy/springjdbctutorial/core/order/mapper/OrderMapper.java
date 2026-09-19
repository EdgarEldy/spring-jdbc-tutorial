package com.edgareldy.springjdbctutorial.core.order.mapper;

import com.edgareldy.springjdbctutorial.core.order.dto.OrderDto;
import com.edgareldy.springjdbctutorial.core.order.entity.Order;

/**
 * Converts between the Order entity and the OrderDto.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class OrderMapper {

    public OrderDto toDto(Order order) {
        if (order == null) {
            return null;
        }
        return new OrderDto(order.getId(), order.getCustomerId(), order.getProductId(),
                order.getQuantity(), order.getTotal());
    }

    public Order toEntity(OrderDto dto) {
        if (dto == null) {
            return null;
        }
        return new Order(dto.getId(), dto.getCustomerId(), dto.getProductId(),
                dto.getQuantity(), dto.getTotal());
    }
}
