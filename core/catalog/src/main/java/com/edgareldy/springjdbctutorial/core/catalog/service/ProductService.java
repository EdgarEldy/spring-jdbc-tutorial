package com.edgareldy.springjdbctutorial.core.catalog.service;

import com.edgareldy.springjdbctutorial.core.catalog.dto.ProductDto;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;

/**
 * Business operations on products. Dto types only at this boundary.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public interface ProductService {

    /** One page of products, optionally restricted to a category (categoryId may be null). */
    PageDto<ProductDto> list(int page, int size, Long categoryId);

    /** Throws ResourceNotFoundException when the id is unknown. */
    ProductDto get(Long id);

    /** The category must exist. The unit price is normalised to two decimals. */
    ProductDto create(ProductDto input);

    ProductDto update(Long id, ProductDto input);

    /** Refused with BusinessRuleException when orders reference the product. */
    void delete(Long id);
}
