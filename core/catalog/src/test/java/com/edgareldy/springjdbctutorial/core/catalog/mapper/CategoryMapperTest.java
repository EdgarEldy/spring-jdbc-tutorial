package com.edgareldy.springjdbctutorial.core.catalog.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springjdbctutorial.core.catalog.dto.CategoryDto;
import com.edgareldy.springjdbctutorial.core.catalog.entity.Category;
import org.junit.jupiter.api.Test;

/**
 * Tests CategoryMapper (entity to dto and back) on hand-built objects, no mocks.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
class CategoryMapperTest {

    private final CategoryMapper mapper = new CategoryMapper();

    @Test
    void _01_ShouldCopyIdAndName_WhenConvertingEntityToDto() {
        CategoryDto dto = mapper.toDto(new Category(3L, "Books"));

        assertThat(dto.getId()).isEqualTo(3L);
        assertThat(dto.getCategoryName()).isEqualTo("Books");
    }

    @Test
    void _02_ShouldReturnNull_WhenEntityIsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void _03_ShouldCopyIdAndName_WhenConvertingDtoToEntity() {
        Category category = mapper.toEntity(new CategoryDto(3L, "Books"));

        assertThat(category.getId()).isEqualTo(3L);
        assertThat(category.getCategoryName()).isEqualTo("Books");
    }

    @Test
    void _04_ShouldReturnNull_WhenDtoIsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
