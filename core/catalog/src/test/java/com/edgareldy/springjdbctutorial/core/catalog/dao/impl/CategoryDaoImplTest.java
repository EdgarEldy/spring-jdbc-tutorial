package com.edgareldy.springjdbctutorial.core.catalog.dao.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springjdbctutorial.core.catalog.config.DaoConfig;
import com.edgareldy.springjdbctutorial.core.catalog.dao.CategoryDao;
import com.edgareldy.springjdbctutorial.core.catalog.entity.Category;
import com.edgareldy.springjdbctutorial.core.common.config.DataSourceConfig;
import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests CategoryDaoImpl against the real PostgreSQL, with its own fixture file category-dao-dataset.sql.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
// Testcontainers starts a throw-away PostgreSQL in Docker (shared by every test class of the JVM through
// PostgresTestContainer); @DynamicPropertySource injects its url into the Spring Environment before the context
// is built, so DataSourceConfig connects to it and Flyway migrates the REAL schema. @Sql runs this class's own
// fixture file before EACH test so every test starts from the same rows.
@SpringJUnitConfig({DataSourceConfig.class, DaoConfig.class})
@Sql("/category-dao-dataset.sql")
class CategoryDaoImplTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private CategoryDao categoryDao;

    @Test
    void _01_ShouldReturnCategory_WhenIdExists() {
        assertThat(categoryDao.findById(2L)).get().extracting(Category::getCategoryName).isEqualTo("Games");
    }

    @Test
    void _02_ShouldReturnEmpty_WhenIdDoesNotExist() {
        assertThat(categoryDao.findById(999L)).isEmpty();
    }

    @Test
    void _03_ShouldReturnFirstPageOrderedById_WhenPageIsZero() {
        assertThat(categoryDao.findPage(0, 2)).extracting(Category::getId).containsExactly(1L, 2L);
    }

    @Test
    void _04_ShouldApplyOffsetOfPageTimesSize_WhenPageIsOne() {
        assertThat(categoryDao.findPage(1, 2)).extracting(Category::getId).containsExactly(3L);
    }

    @Test
    void _05_ShouldReturnEmptyList_WhenPageIsBeyondTheLastOne() {
        assertThat(categoryDao.findPage(50, 10)).isEmpty();
    }

    @Test
    void _06_ShouldCountEveryRow_WhenCountingAll() {
        assertThat(categoryDao.countAll()).isEqualTo(3L);
    }

    @Test
    void _07_ShouldReportExistence_WhenCheckingByName() {
        assertThat(categoryDao.existsByCategoryName("Books")).isTrue();
        assertThat(categoryDao.existsByCategoryName("Nothing")).isFalse();
    }

    @Test
    void _08_ShouldGenerateIdAndPersistRow_WhenCategoryIsInserted() {
        Category created = categoryDao.insert(new Category(null, "Music"));

        assertThat(created.getId()).isNotNull().isGreaterThanOrEqualTo(100L);
        assertThat(categoryDao.findById(created.getId())).get().extracting(Category::getCategoryName).isEqualTo("Music");
        assertThat(categoryDao.countAll()).isEqualTo(4L);
    }

    @Test
    void _09_ShouldThrowDuplicateKeyException_WhenCategoryNameAlreadyExists() {
        assertThatThrownBy(() -> categoryDao.insert(new Category(null, "Books")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void _10_ShouldRenameCategory_WhenUpdated() {
        categoryDao.update(new Category(2L, "Video games"));

        assertThat(categoryDao.findById(2L)).get().extracting(Category::getCategoryName).isEqualTo("Video games");
    }

    @Test
    void _11_ShouldThrowDuplicateKeyException_WhenRenamingToAnExistingName() {
        assertThatThrownBy(() -> categoryDao.update(new Category(2L, "Books")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void _12_ShouldDeleteCategory_WhenNoProductReferencesIt() {
        categoryDao.delete(1L);

        assertThat(categoryDao.findById(1L)).isEmpty();
        assertThat(categoryDao.countAll()).isEqualTo(2L);
    }

    @Test
    void _13_ShouldThrowDataIntegrityViolationException_WhenAProductStillReferencesTheCategory() {
        assertThatThrownBy(() -> categoryDao.delete(3L)).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(categoryDao.findById(3L)).isPresent();
    }
}
