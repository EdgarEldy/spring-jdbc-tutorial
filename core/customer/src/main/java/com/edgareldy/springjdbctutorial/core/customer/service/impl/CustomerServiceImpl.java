package com.edgareldy.springjdbctutorial.core.customer.service.impl;

import com.edgareldy.springjdbctutorial.core.common.dto.PageDto;
import com.edgareldy.springjdbctutorial.core.common.exception.BusinessRuleException;
import com.edgareldy.springjdbctutorial.core.common.exception.ResourceNotFoundException;
import com.edgareldy.springjdbctutorial.core.customer.dao.CustomerDao;
import com.edgareldy.springjdbctutorial.core.customer.dto.CustomerDto;
import com.edgareldy.springjdbctutorial.core.customer.entity.Customer;
import com.edgareldy.springjdbctutorial.core.customer.mapper.CustomerMapper;
import com.edgareldy.springjdbctutorial.core.customer.service.CustomerService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of CustomerService. Declared by @Bean in ServiceConfig (no stereotype) and returned as the interface so the @Transactional advice applies through the proxy.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class CustomerServiceImpl implements CustomerService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_TELEPHONE_LENGTH = 30;
    private static final int MAX_EMAIL_LENGTH = 255;
    private static final int MAX_ADDRESS_LENGTH = 255;

    private final CustomerDao customerDao;
    private final CustomerMapper mapper = new CustomerMapper();

    public CustomerServiceImpl(CustomerDao customerDao) {
        this.customerDao = customerDao;
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<CustomerDto> list(int page, int size) {
        if (page < 0) {
            throw new BusinessRuleException("Page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessRuleException("Size must be between 1 and " + MAX_PAGE_SIZE);
        }
        List<CustomerDto> content = new ArrayList<>();
        for (Customer customer : customerDao.findPage(page, size)) {
            content.add(mapper.toDto(customer));
        }
        return new PageDto<>(content, page, size, customerDao.countAll());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDto get(Long id) {
        return mapper.toDto(load(id));
    }

    @Override
    @Transactional
    public CustomerDto create(CustomerDto input) {
        return mapper.toDto(customerDao.insert(clean(null, input)));
    }

    @Override
    @Transactional
    public CustomerDto update(Long id, CustomerDto input) {
        load(id);
        Customer customer = clean(id, input);
        if (customerDao.update(customer) == 0) {
            // The customer was deleted between the load and the update
            throw new ResourceNotFoundException("Customer not found: " + id);
        }
        return mapper.toDto(customer);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        load(id);
        try {
            customerDao.delete(id);
        } catch (DataIntegrityViolationException e) {
            // The foreign key from orders refused the delete
            throw new BusinessRuleException("Customer is referenced by orders", e);
        }
    }

    private Customer load(Long id) {
        return customerDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private static Customer clean(Long id, CustomerDto input) {
        String firstName = required(input.getFirstName(), "First name", MAX_NAME_LENGTH);
        String lastName = required(input.getLastName(), "Last name", MAX_NAME_LENGTH);
        String telephone = optional(input.getTelephone(), "Telephone", MAX_TELEPHONE_LENGTH);
        String email = optional(input.getEmail(), "Email", MAX_EMAIL_LENGTH);
        if (email != null && !isValidEmail(email)) {
            throw new BusinessRuleException("Email is not valid");
        }
        String address = optional(input.getAddress(), "Address", MAX_ADDRESS_LENGTH);
        return new Customer(id, firstName, lastName, telephone, email, address);
    }

    private static String required(String raw, String label, int max) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new BusinessRuleException(label + " must not be blank");
        }
        return checkLength(value, label, max);
    }

    /** Trims, turns blank into null, then checks the length. */
    private static String optional(String raw, String label, int max) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        return checkLength(value, label, max);
    }

    private static String checkLength(String value, String label, int max) {
        if (value.length() > max) {
            throw new BusinessRuleException(label + " must be at most " + max + " characters");
        }
        return value;
    }

    /** Basic format: no whitespace, one @, a non-empty local part and a domain containing a dot. */
    private static boolean isValidEmail(String email) {
        for (int i = 0; i < email.length(); i++) {
            if (Character.isWhitespace(email.charAt(i))) {
                return false;
            }
        }
        int at = email.indexOf('@');
        if (at < 1 || at != email.lastIndexOf('@')) {
            return false;
        }
        String domain = email.substring(at + 1);
        int dot = domain.indexOf('.');
        return dot > 0 && !domain.endsWith(".");
    }
}
