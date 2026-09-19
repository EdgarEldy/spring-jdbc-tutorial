package com.edgareldy.springjdbctutorial.core.common.dao.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springjdbctutorial.core.common.config.DataSourceConfig;
import com.edgareldy.springjdbctutorial.core.common.support.PostgresTestContainer;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Tests the shared query helpers of AbstractDao (findOne, insertReturningId, count) on the categories
 * table, with its own fixture file abstract-dao-dataset.sql.
 * <p>
 * Created edgar.muhamyangabo on 9/19/26
 * Author : edgar.muhamyangabo
 * Date : 9/19/26
 * Project : spring-jdbc-tutorial
 */
// @Sql runs the fixture script before EACH test method, so every test starts from the same known rows.
// The fixture is dedicated to this class and never reused by another DAO test class.
@SpringJUnitConfig(DataSourceConfig.class)
@Sql("/abstract-dao-dataset.sql")
class AbstractDaoTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        PostgresTestContainer.register(registry);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Minimal concrete DAO exposing the protected helpers under test. */
    private static final class CategoryProbeDao extends AbstractDao {

        CategoryProbeDao(JdbcTemplate jdbcTemplate) {
            super(jdbcTemplate);
        }

        Optional<String> findName(long id) {
            return findOne("SELECT category_name FROM categories WHERE id = ?",
                    (rs, rowNum) -> rs.getString("category_name"), id);
        }

        long insert(String name) {
            return insertReturningId("INSERT INTO categories (category_name) VALUES (?) RETURNING id", name);
        }

        long total() {
            return count("SELECT COUNT(*) FROM categories");
        }
    }

    @Test
    void _01_ShouldReturnRow_WhenFindOneMatchesARow() {
        assertThat(new CategoryProbeDao(jdbcTemplate).findName(1L)).contains("Books");
    }

    @Test
    void _02_ShouldReturnEmpty_WhenFindOneMatchesNoRow() {
        assertThat(new CategoryProbeDao(jdbcTemplate).findName(999L)).isEmpty();
    }

    @Test
    void _03_ShouldReturnIncreasingIds_WhenInsertingTwice() {
        CategoryProbeDao dao = new CategoryProbeDao(jdbcTemplate);

        long first = dao.insert("Music");
        long second = dao.insert("Games");

        assertThat(first).isGreaterThanOrEqualTo(100L);
        assertThat(second).isGreaterThan(first);
    }

    @Test
    void _04_ShouldCountFixtureRows_WhenCounting() {
        assertThat(new CategoryProbeDao(jdbcTemplate).total()).isEqualTo(2L);
    }
}
