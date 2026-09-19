package com.edgareldy.springjdbctutorial.ws.converter;

import com.edgareldy.springjdbctutorial.core.catalog.dto.ProductDto;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.ws.payload.catalog.ProductRequest;
import com.edgareldy.springjdbctutorial.ws.payload.catalog.ProductResponse;
import com.edgareldy.springjdbctutorial.ws.payload.common.PageResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts between the core ProductDto and the product HTTP payloads.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public final class ProductConverter {

    private ProductConverter() {
    }

    public static ProductDto toDto(ProductRequest request) {
        ProductDto dto = new ProductDto();
        dto.setCategoryId(request.getCategoryId());
        dto.setProductName(request.getProductName());
        dto.setUnitPrice(request.getUnitPrice());
        return dto;
    }

    public static ProductResponse toResponse(ProductDto dto) {
        return new ProductResponse(dto.getId(), dto.getCategoryId(), dto.getProductName(), dto.getUnitPrice());
    }

    public static PageResponse<ProductResponse> toPageResponse(PageDto<ProductDto> page) {
        List<ProductResponse> content = new ArrayList<>();
        for (ProductDto dto : page.getContent()) {
            content.add(toResponse(dto));
        }
        return PageResponse.of(content, page.getPage(), page.getSize(), page.getTotalElements());
    }
}
