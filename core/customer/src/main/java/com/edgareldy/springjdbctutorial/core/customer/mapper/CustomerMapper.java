package com.edgareldy.springjdbctutorial.core.customer.mapper;

import com.edgareldy.springjdbctutorial.core.customer.dto.CustomerDto;
import com.edgareldy.springjdbctutorial.core.customer.entity.Customer;

/**
 * Converts between the Customer entity and the CustomerDto.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class CustomerMapper {

    public CustomerDto toDto(Customer customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerDto(customer.getId(), customer.getFirstName(), customer.getLastName(),
                customer.getTelephone(), customer.getEmail(), customer.getAddress());
    }

    public Customer toEntity(CustomerDto dto) {
        if (dto == null) {
            return null;
        }
        return new Customer(dto.getId(), dto.getFirstName(), dto.getLastName(),
                dto.getTelephone(), dto.getEmail(), dto.getAddress());
    }
}
