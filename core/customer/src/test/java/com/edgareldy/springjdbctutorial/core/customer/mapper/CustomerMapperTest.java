package com.edgareldy.springjdbctutorial.core.customer.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springjdbctutorial.core.customer.dto.CustomerDto;
import com.edgareldy.springjdbctutorial.core.customer.entity.Customer;
import org.junit.jupiter.api.Test;

/**
 * Tests CustomerMapper (entity to dto and back) on hand-built objects, no mocks.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
class CustomerMapperTest {

    private final CustomerMapper mapper = new CustomerMapper();

    @Test
    void _01_ShouldCopyEveryField_WhenConvertingEntityToDto() {
        CustomerDto dto = mapper.toDto(new Customer(3L, "Alice", "Martin", "555", "a@b.co", "Street"));

        assertThat(dto).isEqualTo(new CustomerDto(3L, "Alice", "Martin", "555", "a@b.co", "Street"));
    }

    @Test
    void _02_ShouldKeepNulls_WhenOptionalFieldsAreNull() {
        CustomerDto dto = mapper.toDto(new Customer(3L, "Bob", "Stone", null, null, null));

        assertThat(dto.getTelephone()).isNull();
        assertThat(dto.getEmail()).isNull();
        assertThat(dto.getAddress()).isNull();
    }

    @Test
    void _03_ShouldReturnNull_WhenEntityIsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void _04_ShouldCopyEveryField_WhenConvertingDtoToEntity() {
        Customer customer = mapper.toEntity(new CustomerDto(3L, "Alice", "Martin", "555", "a@b.co", "Street"));

        assertThat(customer).isEqualTo(new Customer(3L, "Alice", "Martin", "555", "a@b.co", "Street"));
    }

    @Test
    void _05_ShouldReturnNull_WhenDtoIsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }
}
