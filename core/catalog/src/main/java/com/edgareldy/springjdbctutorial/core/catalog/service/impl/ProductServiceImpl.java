package com.edgareldy.springjdbctutorial.core.catalog.service.impl;

import com.edgareldy.springjdbctutorial.core.catalog.dao.CategoryDao;
import com.edgareldy.springjdbctutorial.core.catalog.dao.ProductDao;
import com.edgareldy.springjdbctutorial.core.catalog.dto.ProductDto;
import com.edgareldy.springjdbctutorial.core.catalog.entity.Product;
import com.edgareldy.springjdbctutorial.core.catalog.mapper.ProductMapper;
import com.edgareldy.springjdbctutorial.core.catalog.service.ProductService;
import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of ProductService. Declared by @Bean in ServiceConfig (no stereotype). It reads CategoryDao of the same module to check the category exists.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class ProductServiceImpl implements ProductService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_NAME_LENGTH = 150;
    private static final int PRICE_SCALE = 2;
    // NUMERIC(12, 2) holds at most 10 integer digits
    private static final BigDecimal MAX_PRICE = new BigDecimal("9999999999.99");

    private final ProductDao productDao;
    private final CategoryDao categoryDao;
    private final ProductMapper mapper = new ProductMapper();

    public ProductServiceImpl(ProductDao productDao, CategoryDao categoryDao) {
        this.productDao = productDao;
        this.categoryDao = categoryDao;
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<ProductDto> list(int page, int size, Long categoryId) {
        if (page < 0) {
            throw new BusinessRuleException("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessRuleException("Size must be between 1 and " + MAX_PAGE_SIZE);
        }
        List<ProductDto> content = new ArrayList<>();
        for (Product product : productDao.findPage(page, size, categoryId)) {
            content.add(mapper.toDto(product));
        }
        return new PageDto<>(content, page, size, productDao.count(categoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto get(Long id) {
        return mapper.toDto(load(id));
    }

    @Override
    @Transactional
    public ProductDto create(ProductDto input) {
        String name = cleanName(input.getProductName());
        BigDecimal price = cleanPrice(input.getUnitPrice());
        requireCategory(input.getCategoryId());
        return mapper.toDto(productDao.insert(new Product(null, input.getCategoryId(), name, price)));
    }

    @Override
    @Transactional
    public ProductDto update(Long id, ProductDto input) {
        Product product = load(id);
        String name = cleanName(input.getProductName());
        BigDecimal price = cleanPrice(input.getUnitPrice());
        requireCategory(input.getCategoryId());
        product.setCategoryId(input.getCategoryId());
        product.setProductName(name);
        product.setUnitPrice(price);
        productDao.update(product);
        return mapper.toDto(product);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        load(id);
        try {
            productDao.delete(id);
        } catch (DataIntegrityViolationException e) {
            // The orders table references products: its foreign key refuses the delete
            throw new BusinessRuleException("Product is referenced by orders", e);
        }
    }

    private Product load(Long id) {
        return productDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private void requireCategory(Long categoryId) {
        if (categoryId == null || categoryDao.findById(categoryId).isEmpty()) {
            throw new ResourceNotFoundException("Category not found: " + categoryId);
        }
    }

    private static String cleanName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw new BusinessRuleException("Product name must not be blank");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BusinessRuleException("Product name must be at most " + MAX_NAME_LENGTH + " characters");
        }
        return name;
    }

    /** Rejects null, negative, too large or more than two decimals, then returns the price with exactly two decimals. */
    private static BigDecimal cleanPrice(BigDecimal raw) {
        if (raw == null) {
            throw new BusinessRuleException("Unit price is required");
        }
        if (raw.signum() < 0) {
            throw new BusinessRuleException("Unit price must not be negative");
        }
        if (raw.stripTrailingZeros().scale() > PRICE_SCALE) {
            throw new BusinessRuleException("Unit price must have at most " + PRICE_SCALE + " decimals");
        }
        BigDecimal price = raw.setScale(PRICE_SCALE);
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new BusinessRuleException("Unit price must not exceed " + MAX_PRICE.toPlainString());
        }
        return price;
    }
}
