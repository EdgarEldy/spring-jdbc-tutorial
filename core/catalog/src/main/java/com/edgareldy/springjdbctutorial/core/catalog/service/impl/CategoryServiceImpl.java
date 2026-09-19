package com.edgareldy.springjdbctutorial.core.catalog.service.impl;

import com.edgareldy.springjdbctutorial.core.catalog.dao.CategoryDao;
import com.edgareldy.springjdbctutorial.core.catalog.dao.ProductDao;
import com.edgareldy.springjdbctutorial.core.catalog.dto.CategoryDto;
import com.edgareldy.springjdbctutorial.core.catalog.entity.Category;
import com.edgareldy.springjdbctutorial.core.catalog.mapper.CategoryMapper;
import com.edgareldy.springjdbctutorial.core.catalog.service.CategoryService;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of CategoryService. Declared by @Bean in ServiceConfig (no stereotype) and returned as the interface so the @Transactional advice applies through the proxy.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class CategoryServiceImpl implements CategoryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_NAME_LENGTH = 100;

    private final CategoryDao categoryDao;
    private final ProductDao productDao;
    private final CategoryMapper mapper = new CategoryMapper();

    public CategoryServiceImpl(CategoryDao categoryDao, ProductDao productDao) {
        this.categoryDao = categoryDao;
        this.productDao = productDao;
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<CategoryDto> list(int page, int size) {
        if (page < 0) {
            throw new BusinessRuleException("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessRuleException("Size must be between 1 and " + MAX_PAGE_SIZE);
        }
        List<CategoryDto> content = new ArrayList<>();
        for (Category category : categoryDao.findPage(page, size)) {
            content.add(mapper.toDto(category));
        }
        return new PageDto<>(content, page, size, categoryDao.countAll());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto get(Long id) {
        return mapper.toDto(load(id));
    }

    @Override
    @Transactional
    public CategoryDto create(CategoryDto input) {
        String name = cleanName(input.getCategoryName());
        if (categoryDao.existsByCategoryName(name)) {
            throw new BusinessRuleException("Category name already exists");
        }
        try {
            return mapper.toDto(categoryDao.insert(new Category(null, name)));
        } catch (DuplicateKeyException e) {
            // Lost a race against a concurrent identical insert: the unique constraint decided
            throw new BusinessRuleException("Category name already exists", e);
        }
    }

    @Override
    @Transactional
    public CategoryDto update(Long id, CategoryDto input) {
        Category category = load(id);
        String name = cleanName(input.getCategoryName());
        if (!name.equals(category.getCategoryName()) && categoryDao.existsByCategoryName(name)) {
            throw new BusinessRuleException("Category name already exists");
        }
        category.setCategoryName(name);
        try {
            categoryDao.update(category);
        } catch (DuplicateKeyException e) {
            throw new BusinessRuleException("Category name already exists", e);
        }
        return mapper.toDto(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        load(id);
        if (productDao.countByCategoryId(id) > 0) {
            throw new BusinessRuleException("Category still has products");
        }
        try {
            categoryDao.delete(id);
        } catch (DataIntegrityViolationException e) {
            // A product was added between the check and the delete: the foreign key refused it
            throw new BusinessRuleException("Category still has products", e);
        }
    }

    private Category load(Long id) {
        return categoryDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private static String cleanName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw new BusinessRuleException("Category name must not be blank");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BusinessRuleException("Category name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        return name;
    }
}
