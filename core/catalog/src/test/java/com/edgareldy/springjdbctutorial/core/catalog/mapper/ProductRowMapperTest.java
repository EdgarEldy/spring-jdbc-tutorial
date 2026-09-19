package com.edgareldy.springjdbctutorial.core.catalog.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.catalog.entity.Product;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/**
 * Tests ProductRowMapper against a mocked ResultSet, including the BigDecimal price column.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
class ProductRowMapperTest {

    private final ProductRowMapper mapper = new ProductRowMapper();

    @Test
    void _01_ShouldMapEveryColumnAndKeepThePriceScale_WhenRowIsComplete() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(4L);
        when(rs.getLong("category_id")).thenReturn(2L);
        when(rs.getString("product_name")).thenReturn("Chess");
        when(rs.getBigDecimal("unit_price")).thenReturn(new BigDecimal("12.50"));

        Product product = mapper.mapRow(rs, 0);

        assertThat(product.getId()).isEqualTo(4L);
        assertThat(product.getCategoryId()).isEqualTo(2L);
        assertThat(product.getProductName()).isEqualTo("Chess");
        assertThat(product.getUnitPrice()).isEqualTo(new BigDecimal("12.50"));
        assertThat(product.getUnitPrice().scale()).isEqualTo(2);
    }
}
