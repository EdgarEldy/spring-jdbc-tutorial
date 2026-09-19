package com.edgareldy.springjdbctutorial.core.catalog.dao.impl;

import com.edgareldy.springjdbctutorial.core.catalog.dao.ProductDao;
import com.edgareldy.springjdbctutorial.core.catalog.entity.Product;
import com.edgareldy.springjdbctutorial.core.catalog.mapper.ProductRowMapper;
import com.edgareldy.springjdbctutorial.core.common.dao.support.AbstractDao;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate implementation of ProductDao. Declared by @Bean in DaoConfig, no stereotype.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class ProductDaoImpl extends AbstractDao implements ProductDao {

    private static final String COLUMNS = "SELECT id, category_id, product_name, unit_price FROM products";

    private final ProductRowMapper rowMapper = new ProductRowMapper();

    public ProductDaoImpl(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public List<Product> findPage(int page, int size, Long categoryId) {
        long offset = (long) page * size;
        if (categoryId == null) {
            return getJdbcTemplate().query(COLUMNS + " ORDER BY id LIMIT ? OFFSET ?", rowMapper, size, offset);
        }
        return getJdbcTemplate().query(COLUMNS + " WHERE category_id = ? ORDER BY id LIMIT ? OFFSET ?",
                rowMapper, categoryId, size, offset);
    }

    @Override
    public long count(Long categoryId) {
        if (categoryId == null) {
            return count("SELECT COUNT(*) FROM products");
        }
        return countByCategoryId(categoryId);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return findOne(COLUMNS + " WHERE id = ?", rowMapper, id);
    }

    @Override
    public Product insert(Product product) {
        long id = insertReturningId(
                "INSERT INTO products (category_id, product_name, unit_price) VALUES (?, ?, ?) RETURNING id",
                product.getCategoryId(), product.getProductName(), product.getUnitPrice());
        product.setId(id);
        return product;
    }

    @Override
    public void update(Product product) {
        getJdbcTemplate().update(
                "UPDATE products SET category_id = ?, product_name = ?, unit_price = ? WHERE id = ?",
                product.getCategoryId(), product.getProductName(), product.getUnitPrice(), product.getId());
    }

    @Override
    public void delete(Long id) {
        getJdbcTemplate().update("DELETE FROM products WHERE id = ?", id);
    }

    @Override
    public long countByCategoryId(Long categoryId) {
        return count("SELECT COUNT(*) FROM products WHERE category_id = ?", categoryId);
    }
}
