package com.edgareldy.springjdbctutorial.core.catalog.dao;

import com.edgareldy.springjdbctutorial.core.catalog.entity.Category;

import java.util.List;
import java.util.Optional;

/**
 * Data access for categories. Entity types only.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public interface CategoryDao {

    /** One page of categories ordered by id. */
    List<Category> findPage(int page, int size);

    long countAll();

    Optional<Category> findById(Long id);

    boolean existsByCategoryName(String categoryName);

    /** Inserts the category and returns it with its generated id. */
    Category insert(Category category);

    void update(Category category);

    void delete(Long id);
}
