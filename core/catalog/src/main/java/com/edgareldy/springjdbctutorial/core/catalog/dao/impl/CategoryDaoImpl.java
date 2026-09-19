package com.edgareldy.springjdbctutorial.core.catalog.dao.impl;

import com.edgareldy.springjdbctutorial.core.catalog.dao.CategoryDao;
import com.edgareldy.springjdbctutorial.core.catalog.entity.Category;
import com.edgareldy.springjdbctutorial.core.catalog.mapper.CategoryRowMapper;
import com.edgareldy.springjdbctutorial.core.common.dao.support.AbstractDao;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate implementation of CategoryDao. Declared by @Bean in DaoConfig, no stereotype.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class CategoryDaoImpl extends AbstractDao implements CategoryDao {

    private final CategoryRowMapper rowMapper = new CategoryRowMapper();

    public CategoryDaoImpl(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public List<Category> findPage(int page, int size) {
        return getJdbcTemplate().query(
                "SELECT id, category_name FROM categories ORDER BY id LIMIT ? OFFSET ?",
                rowMapper, size, (long) page * size);
    }

    @Override
    public long countAll() {
        return count("SELECT COUNT(*) FROM categories");
    }

    @Override
    public Optional<Category> findById(Long id) {
        return findOne("SELECT id, category_name FROM categories WHERE id = ?", rowMapper, id);
    }

    @Override
    public boolean existsByCategoryName(String categoryName) {
        return count("SELECT COUNT(*) FROM categories WHERE category_name = ?", categoryName) > 0;
    }

    @Override
    public Category insert(Category category) {
        long id = insertReturningId("INSERT INTO categories (category_name) VALUES (?) RETURNING id",
                category.getCategoryName());
        category.setId(id);
        return category;
    }

    @Override
    public void update(Category category) {
        getJdbcTemplate().update("UPDATE categories SET category_name = ? WHERE id = ?",
                category.getCategoryName(), category.getId());
    }

    @Override
    public void delete(Long id) {
        getJdbcTemplate().update("DELETE FROM categories WHERE id = ?", id);
    }
}
