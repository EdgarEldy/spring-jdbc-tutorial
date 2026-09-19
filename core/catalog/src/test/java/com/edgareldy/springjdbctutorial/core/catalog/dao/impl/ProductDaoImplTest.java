package com.edgareldy.springjdbctutorial.core.catalog.dao.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springjdbctutorial.core.catalog.config.DaoConfig;
import com.edgareldy.springjdbctutorial.core.catalog.dao.ProductDao;
import com.edgareldy.springjdbctutorial.core.catalog.entity.Product;
import com.edgareldy.springjdbctutorial.core.common.config.DataSourceConfig;
import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests ProductDaoImpl against the real PostgreSQL, with its own fixture file product-dao-dataset.sql.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
// Same recipe as CategoryDaoImplTest: Testcontainers PostgreSQL migrated by Flyway, url injected by
// @DynamicPropertySource, this class's own fixture loaded by @Sql before each test.
@SpringJUnitConfig({DataSourceConfig.class, DaoConfig.class})
@Sql("/product-dao-dataset.sql")
class ProductDaoImplTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private ProductDao productDao;

    @Test
    void _01_ShouldReturnProductWithExactPrice_WhenIdExists() {
        Product product = productDao.findById(1L).orElseThrow();

        assertThat(product.getProductName()).isEqualTo("Novel");
        assertThat(product.getCategoryId()).isEqualTo(1L);
        assertThat(product.getUnitPrice()).isEqualTo(new BigDecimal("12.50"));
    }

    @Test
    void _02_ShouldReturnEmpty_WhenIdDoesNotExist() {
        assertThat(productDao.findById(999L)).isEmpty();
    }

    @Test
    void _03_ShouldReturnEveryProductOrderedById_WhenNoFilterIsGiven() {
        assertThat(productDao.findPage(0, 10, null)).extracting(Product::getId).containsExactly(1L, 2L, 3L, 4L);
    }

    @Test
    void _04_ShouldApplyOffsetOfPageTimesSize_WhenPagingWithoutFilter() {
        assertThat(productDao.findPage(1, 3, null)).extracting(Product::getId).containsExactly(4L);
        assertThat(productDao.findPage(9, 3, null)).isEmpty();
    }

    @Test
    void _05_ShouldKeepOnlyTheProductsOfThatCategory_WhenFilteringByCategoryId() {
        assertThat(productDao.findPage(0, 10, 2L)).extracting(Product::getId).containsExactly(3L, 4L);
        assertThat(productDao.findPage(1, 1, 2L)).extracting(Product::getId).containsExactly(4L);
    }

    @Test
    void _06_ShouldReturnEmptyList_WhenFilteredCategoryHasNoProduct() {
        assertThat(productDao.findPage(0, 10, 3L)).isEmpty();
    }

    @Test
    void _07_ShouldCountWithAndWithoutFilter_WhenCounting() {
        assertThat(productDao.count(null)).isEqualTo(4L);
        assertThat(productDao.count(1L)).isEqualTo(2L);
        assertThat(productDao.count(3L)).isZero();
    }

    @Test
    void _08_ShouldCountTheProductsOfTheCategory_WhenCountingByCategoryId() {
        assertThat(productDao.countByCategoryId(2L)).isEqualTo(2L);
        assertThat(productDao.countByCategoryId(3L)).isZero();
    }

    @Test
    void _09_ShouldGenerateIdAndKeepScale_WhenProductIsInserted() {
        Product created = productDao.insert(new Product(null, 3L, "Lamp", new BigDecimal("12.50")));

        assertThat(created.getId()).isNotNull().isGreaterThanOrEqualTo(100L);
        Product reloaded = productDao.findById(created.getId()).orElseThrow();
        // NUMERIC(12, 2) round trip: 12.50 comes back as 12.50 (scale 2), not 12.5
        assertThat(reloaded.getUnitPrice()).isEqualTo(new BigDecimal("12.50"));
        assertThat(reloaded.getUnitPrice().scale()).isEqualTo(2);
        assertThat(reloaded.getCategoryId()).isEqualTo(3L);
    }

    @Test
    void _10_ShouldThrowDataIntegrityViolationException_WhenCategoryDoesNotExist() {
        assertThatThrownBy(() -> productDao.insert(new Product(null, 999L, "Ghost", new BigDecimal("1.00"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void _11_ShouldThrowDataIntegrityViolationException_WhenUnitPriceIsNegative() {
        assertThatThrownBy(() -> productDao.insert(new Product(null, 1L, "Broken", new BigDecimal("-0.01"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void _12_ShouldAcceptZeroPrice_WhenInserting() {
        Product created = productDao.insert(new Product(null, 1L, "Free", BigDecimal.ZERO));

        assertThat(productDao.findById(created.getId()).orElseThrow().getUnitPrice()).isEqualByComparingTo("0");
    }

    @Test
    void _13_ShouldUpdateEveryColumn_WhenProductIsUpdated() {
        productDao.update(new Product(1L, 2L, "Novel 2", new BigDecimal("15.75")));

        Product reloaded = productDao.findById(1L).orElseThrow();
        assertThat(reloaded.getCategoryId()).isEqualTo(2L);
        assertThat(reloaded.getProductName()).isEqualTo("Novel 2");
        assertThat(reloaded.getUnitPrice()).isEqualTo(new BigDecimal("15.75"));
    }

    @Test
    void _14_ShouldThrowDataIntegrityViolationException_WhenUpdatingToAnUnknownCategory() {
        assertThatThrownBy(() -> productDao.update(new Product(1L, 999L, "Novel", new BigDecimal("12.50"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void _15_ShouldDeleteProduct_WhenIdExists() {
        productDao.delete(4L);

        assertThat(productDao.findById(4L)).isEmpty();
        assertThat(productDao.count(null)).isEqualTo(3L);
    }
}
