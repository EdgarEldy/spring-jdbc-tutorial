package com.edgareldy.springjdbctutorial.core.customer.service;

import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.customer.dto.CustomerDto;

/**
 * Business operations on customers. Dto types only at this boundary.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public interface CustomerService {

    /** One page of customers. Page must be >= 0 and size in 1..100, else BusinessRuleException. */
    PageDto<CustomerDto> list(int page, int size);

    /** Throws ResourceNotFoundException when the id is unknown. */
    CustomerDto get(Long id);

    /** Names are required, telephone, email and address are optional (blank becomes null). */
    CustomerDto create(CustomerDto input);

    CustomerDto update(Long id, CustomerDto input);

    /** Refused with BusinessRuleException while the customer is referenced by orders. */
    void delete(Long id);
}
