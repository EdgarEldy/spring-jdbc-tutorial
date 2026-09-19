package com.edgareldy.springjdbctutorial.ws.converter;

import com.edgareldy.springjdbctutorial.core.catalog.dto.CategoryDto;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.ws.payload.catalog.CategoryRequest;
import com.edgareldy.springjdbctutorial.ws.payload.catalog.CategoryResponse;
import com.edgareldy.springjdbctutorial.ws.payload.common.PageResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts between the core CategoryDto and the category HTTP payloads.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public final class CategoryConverter {

    private CategoryConverter() {
    }

    public static CategoryDto toDto(CategoryRequest request) {
        CategoryDto dto = new CategoryDto();
        dto.setCategoryName(request.getCategoryName());
        return dto;
    }

    public static CategoryResponse toResponse(CategoryDto dto) {
        return new CategoryResponse(dto.getId(), dto.getCategoryName());
    }

    public static PageResponse<CategoryResponse> toPageResponse(PageDto<CategoryDto> page) {
        List<CategoryResponse> content = new ArrayList<>();
        for (CategoryDto dto : page.getContent()) {
            content.add(toResponse(dto));
        }
        return PageResponse.of(content, page.getPage(), page.getSize(), page.getTotalElements());
    }
}
