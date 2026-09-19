package com.edgareldy.springjdbctutorial.core.catalog.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.catalog.entity.Category;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/**
 * Tests CategoryRowMapper against a mocked ResultSet, without any database.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
class CategoryRowMapperTest {

    private final CategoryRowMapper mapper = new CategoryRowMapper();

    // Mockito.mock(ResultSet.class) is a fake row whose getters return what the test stubs, column by
    // column: the test only checks the column-to-field mapping, with no database and no driver.
    @Test
    void _01_ShouldMapEveryColumn_WhenRowIsComplete() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(7L);
        when(rs.getString("category_name")).thenReturn("Books");

        Category category = mapper.mapRow(rs, 0);

        assertThat(category.getId()).isEqualTo(7L);
        assertThat(category.getCategoryName()).isEqualTo("Books");
    }
}
