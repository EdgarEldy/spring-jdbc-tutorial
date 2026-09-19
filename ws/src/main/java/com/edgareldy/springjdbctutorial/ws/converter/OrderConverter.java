package com.edgareldy.springjdbctutorial.ws.converter;

import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.order.dto.OrderDto;
import com.edgareldy.springjdbctutorial.ws.payload.common.PageResponse;
import com.edgareldy.springjdbctutorial.ws.payload.order.OrderRequest;
import com.edgareldy.springjdbctutorial.ws.payload.order.OrderResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts between the core OrderDto and the order HTTP payloads.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public final class OrderConverter {

    private OrderConverter() {
    }

    public static OrderDto toDto(OrderRequest request) {
        OrderDto dto = new OrderDto();
        dto.setCustomerId(request.getCustomerId());
        dto.setProductId(request.getProductId());
        dto.setQuantity(request.getQuantity());
        return dto;
    }

    public static OrderResponse toResponse(OrderDto dto) {
        return new OrderResponse(dto.getId(), dto.getCustomerId(), dto.getProductId(),
                dto.getQuantity(), dto.getTotal());
    }

    public static PageResponse<OrderResponse> toPageResponse(PageDto<OrderDto> page) {
        List<OrderResponse> content = new ArrayList<>();
        for (OrderDto dto : page.getContent()) {
            content.add(toResponse(dto));
        }
        return PageResponse.of(content, page.getPage(), page.getSize(), page.getTotalElements());
    }
}
