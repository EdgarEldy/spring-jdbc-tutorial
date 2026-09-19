package com.edgareldy.springjdbctutorial.core.catalog.service;

import com.edgareldy.springjdbctutorial.core.catalog.dto.CategoryDto;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;

/**
 * Business operations on categories. Dto types only at this boundary.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public interface CategoryService {

    /** One page of categories. Page must be >= 0 and size in 1..100, else BusinessRuleException. */
    PageDto<CategoryDto> list(int page, int size);

    /** Throws ResourceNotFoundException when the id is unknown. */
    CategoryDto get(Long id);

    /** Only the category name of the input is read. Names are trimmed and unique. */
    CategoryDto create(CategoryDto input);

    CategoryDto update(Long id, CategoryDto input);

    /** Refused with BusinessRuleException while the category still has products. */
    void delete(Long id);
}
