package com.edgareldy.springjdbctutorial.ws.converter;

import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.customer.dto.CustomerDto;
import com.edgareldy.springjdbctutorial.ws.payload.common.PageResponse;
import com.edgareldy.springjdbctutorial.ws.payload.customer.CustomerRequest;
import com.edgareldy.springjdbctutorial.ws.payload.customer.CustomerResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts between the core CustomerDto and the customer HTTP payloads.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public final class CustomerConverter {

    private CustomerConverter() {
    }

    public static CustomerDto toDto(CustomerRequest request) {
        CustomerDto dto = new CustomerDto();
        dto.setFirstName(request.getFirstName());
        dto.setLastName(request.getLastName());
        dto.setTelephone(request.getTelephone());
        dto.setEmail(request.getEmail());
        dto.setAddress(request.getAddress());
        return dto;
    }

    public static CustomerResponse toResponse(CustomerDto dto) {
        return new CustomerResponse(dto.getId(), dto.getFirstName(), dto.getLastName(),
                dto.getTelephone(), dto.getEmail(), dto.getAddress());
    }

    public static PageResponse<CustomerResponse> toPageResponse(PageDto<CustomerDto> page) {
        List<CustomerResponse> content = new ArrayList<>();
        for (CustomerDto dto : page.getContent()) {
            content.add(toResponse(dto));
        }
        return PageResponse.of(content, page.getPage(), page.getSize(), page.getTotalElements());
    }
}
