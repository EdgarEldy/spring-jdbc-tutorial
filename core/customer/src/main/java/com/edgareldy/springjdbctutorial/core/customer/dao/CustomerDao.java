package com.edgareldy.springjdbctutorial.core.customer.dao;

import com.edgareldy.springjdbctutorial.core.customer.entity.Customer;

import java.util.List;
import java.util.Optional;

/**
 * Data access for customers. Entity types only.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public interface CustomerDao {

    /** One page of customers ordered by id. */
    List<Customer> findPage(int page, int size);

    long countAll();

    Optional<Customer> findById(Long id);

    /** Inserts the customer and returns it with its generated id. */
    Customer insert(Customer customer);

    /** Returns the number of rows affected, 0 when the customer no longer exists. */
    int update(Customer customer);

    void delete(Long id);
}
