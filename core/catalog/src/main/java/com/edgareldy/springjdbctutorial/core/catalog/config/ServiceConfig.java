package com.edgareldy.springjdbctutorial.core.catalog.config;

import com.edgareldy.springjdbctutorial.core.catalog.dao.CategoryDao;
import com.edgareldy.springjdbctutorial.core.catalog.dao.ProductDao;
import com.edgareldy.springjdbctutorial.core.catalog.service.CategoryService;
import com.edgareldy.springjdbctutorial.core.catalog.service.ProductService;
import com.edgareldy.springjdbctutorial.core.catalog.service.impl.CategoryServiceImpl;
import com.edgareldy.springjdbctutorial.core.catalog.service.impl.ProductServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Declares the service beans of the catalog module. Imports its DaoConfig so importing this class alone is enough.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
@Configuration
@Import(DaoConfig.class)
public class ServiceConfig {

    @Bean
    public CategoryService categoryService(CategoryDao categoryDao, ProductDao productDao) {
        return new CategoryServiceImpl(categoryDao, productDao);
    }

    @Bean
    public ProductService productService(ProductDao productDao, CategoryDao categoryDao) {
        return new ProductServiceImpl(productDao, categoryDao);
    }
}
