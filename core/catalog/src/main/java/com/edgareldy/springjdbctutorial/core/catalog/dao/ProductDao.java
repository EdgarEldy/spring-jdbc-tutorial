package com.edgareldy.springjdbctutorial.core.catalog.dao;

import com.edgareldy.springjdbctutorial.core.catalog.entity.Product;

import java.util.List;
import java.util.Optional;

/**
 * Data access for products. Entity types only.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public interface ProductDao {

    /** One page of products ordered by id, restricted to a category when categoryId is not null. */
    List<Product> findPage(int page, int size, Long categoryId);

    /** Number of products, restricted to a category when categoryId is not null. */
    long count(Long categoryId);

    Optional<Product> findById(Long id);

    /** Inserts the product and returns it with its generated id. */
    Product insert(Product product);

    void update(Product product);

    void delete(Long id);

    long countByCategoryId(Long categoryId);
}
