package com.edgareldy.springjdbctutorial.core.catalog.mapper;

import com.edgareldy.springjdbctutorial.core.catalog.dto.CategoryDto;
import com.edgareldy.springjdbctutorial.core.catalog.entity.Category;

/**
 * Converts between the Category entity and the CategoryDto.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class CategoryMapper {

    public CategoryDto toDto(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryDto(category.getId(), category.getCategoryName());
    }

    public Category toEntity(CategoryDto dto) {
        if (dto == null) {
            return null;
        }
        return new Category(dto.getId(), dto.getCategoryName());
    }
}
