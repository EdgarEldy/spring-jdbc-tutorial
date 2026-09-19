package com.edgareldy.springjdbctutorial.core.catalog.mapper;

import com.edgareldy.springjdbctutorial.core.catalog.dto.ProductDto;
import com.edgareldy.springjdbctutorial.core.catalog.entity.Product;

/**
 * Converts between the Product entity and the ProductDto.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class ProductMapper {

    public ProductDto toDto(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductDto(product.getId(), product.getCategoryId(), product.getProductName(),
                product.getUnitPrice());
    }

    public Product toEntity(ProductDto dto) {
        if (dto == null) {
            return null;
        }
        return new Product(dto.getId(), dto.getCategoryId(), dto.getProductName(), dto.getUnitPrice());
    }
}
