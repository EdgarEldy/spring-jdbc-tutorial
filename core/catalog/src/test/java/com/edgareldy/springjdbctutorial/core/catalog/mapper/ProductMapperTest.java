package com.edgareldy.springjdbctutorial.core.catalog.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springjdbctutorial.core.catalog.dto.ProductDto;
import com.edgareldy.springjdbctutorial.core.catalog.entity.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Tests ProductMapper (entity to dto and back) on hand-built objects, no mocks.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapper();

    @Test
    void _01_ShouldCopyEveryField_WhenConvertingEntityToDto() {
        ProductDto dto = mapper.toDto(new Product(3L, 2L, "Chess", new BigDecimal("25.99")));

        assertThat(dto.getId()).isEqualTo(3L);
        assertThat(dto.getCategoryId()).isEqualTo(2L);
        assertThat(dto.getProductName()).isEqualTo("Chess");
        assertThat(dto.getUnitPrice()).isEqualTo(new BigDecimal("25.99"));
    }

    @Test
    void _02_ShouldReturnNull_WhenEntityIsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void _03_ShouldCopyEveryField_WhenConvertingDtoToEntity() {
        Product product = mapper.toEntity(new ProductDto(3L, 2L, "Chess", new BigDecimal("25.99")));

        assertThat(product.getId()).isEqualTo(3L);
        assertThat(product.getCategoryId()).isEqualTo(2L);
        assertThat(product.getProductName()).isEqualTo("Chess");
        assertThat(product.getUnitPrice()).isEqualTo(new BigDecimal("25.99"));
    }

    @Test
    void _04_ShouldReturnNull_WhenDtoIsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
