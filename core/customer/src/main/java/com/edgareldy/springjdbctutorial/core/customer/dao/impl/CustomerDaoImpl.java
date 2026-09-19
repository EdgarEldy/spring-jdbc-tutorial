package com.edgareldy.springjdbctutorial.core.customer.dao.impl;

import com.edgareldy.springjdbctutorial.core.common.dao.support.AbstractDao;
import com.edgareldy.springjdbctutorial.core.customer.dao.CustomerDao;
import com.edgareldy.springjdbctutorial.core.customer.entity.Customer;
import com.edgareldy.springjdbctutorial.core.customer.mapper.CustomerRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate implementation of CustomerDao. Declared by @Bean in DaoConfig, no stereotype.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class CustomerDaoImpl extends AbstractDao implements CustomerDao {

    private static final String COLUMNS = "id, first_name, last_name, telephone, email, address";

    private final CustomerRowMapper rowMapper = new CustomerRowMapper();

    public CustomerDaoImpl(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public List<Customer> findPage(int page, int size) {
        return getJdbcTemplate().query(
                "SELECT " + COLUMNS + " FROM customers ORDER BY id LIMIT ? OFFSET ?",
                rowMapper, size, (long) page * size);
    }

    @Override
    public long countAll() {
        return count("SELECT COUNT(*) FROM customers");
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return findOne("SELECT " + COLUMNS + " FROM customers WHERE id = ?", rowMapper, id);
    }

    @Override
    public Customer insert(Customer customer) {
        long id = insertReturningId(
                "INSERT INTO customers (first_name, last_name, telephone, email, address) "
                        + "VALUES (?, ?, ?, ?, ?) RETURNING id",
                customer.getFirstName(), customer.getLastName(), customer.getTelephone(),
                customer.getEmail(), customer.getAddress());
        customer.setId(id);
        return customer;
    }

    @Override
    public int update(Customer customer) {
        return getJdbcTemplate().update(
                "UPDATE customers SET first_name = ?, last_name = ?, telephone = ?, email = ?, address = ? WHERE id = ?",
                customer.getFirstName(), customer.getLastName(), customer.getTelephone(),
                customer.getEmail(), customer.getAddress(), customer.getId());
    }

    @Override
    public void delete(Long id) {
        getJdbcTemplate().update("DELETE FROM customers WHERE id = ?", id);
    }
}
