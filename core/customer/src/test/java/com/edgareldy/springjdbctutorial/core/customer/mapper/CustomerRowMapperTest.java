package com.edgareldy.springjdbctutorial.core.customer.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edgareldy.springjdbctutorial.core.customer.entity.Customer;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

/**
 * Tests CustomerRowMapper against a mocked ResultSet, without any database.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
class CustomerRowMapperTest {

    private final CustomerRowMapper mapper = new CustomerRowMapper();

    // Mockito.mock(ResultSet.class) is a fake row whose getters return what the test stubs, column by
    // column: the test only checks the column-to-field mapping, with no database and no driver.
    @Test
    void _01_ShouldMapEveryColumn_WhenRowIsComplete() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(7L);
        when(rs.getString("first_name")).thenReturn("Alice");
        when(rs.getString("last_name")).thenReturn("Martin");
        when(rs.getString("telephone")).thenReturn("555");
        when(rs.getString("email")).thenReturn("alice@example.com");
        when(rs.getString("address")).thenReturn("1 Main Street");

        Customer customer = mapper.mapRow(rs, 0);

        assertThat(customer).isEqualTo(new Customer(7L, "Alice", "Martin", "555", "alice@example.com", "1 Main Street"));
    }

    @Test
    void _02_ShouldKeepNulls_WhenTelephoneEmailAndAddressAreNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("id")).thenReturn(8L);
        when(rs.getString("first_name")).thenReturn("Bob");
        when(rs.getString("last_name")).thenReturn("Stone");
        when(rs.getString("telephone")).thenReturn(null);
        when(rs.getString("email")).thenReturn(null);
        when(rs.getString("address")).thenReturn(null);

        Customer customer = mapper.mapRow(rs, 0);

        assertThat(customer.getId()).isEqualTo(8L);
        assertThat(customer.getTelephone()).isNull();
        assertThat(customer.getEmail()).isNull();
        assertThat(customer.getAddress()).isNull();
    }
}
