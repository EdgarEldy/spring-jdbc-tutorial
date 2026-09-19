package com.edgareldy.springjdbctutorial.core.order.dao.impl;

import com.edgareldy.springjdbctutorial.core.common.dao.support.AbstractDao;
import com.edgareldy.springjdbctutorial.core.order.dao.OrderDao;
import com.edgareldy.springjdbctutorial.core.order.entity.Order;
import com.edgareldy.springjdbctutorial.core.order.mapper.OrderRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JdbcTemplate implementation of OrderDao. Declared by @Bean in DaoConfig, no stereotype.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class OrderDaoImpl extends AbstractDao implements OrderDao {

    private static final String SELECT = "SELECT id, customer_id, product_id, quantity, total FROM orders";

    private final OrderRowMapper rowMapper = new OrderRowMapper();

    public OrderDaoImpl(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    @Override
    public List<Order> findPage(int page, int size, Long customerId, Long productId) {
        List<Object> args = new ArrayList<>();
        String where = where(customerId, productId, args);
        args.add(size);
        args.add((long) page * size);
        return getJdbcTemplate().query(SELECT + where + " ORDER BY id LIMIT ? OFFSET ?", rowMapper, args.toArray());
    }

    @Override
    public long count(Long customerId, Long productId) {
        List<Object> args = new ArrayList<>();
        String where = where(customerId, productId, args);
        Long total = getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM orders" + where, Long.class, args.toArray());
        return total == null ? 0L : total;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return findOne(SELECT + " WHERE id = ?", rowMapper, id);
    }

    @Override
    public Order insert(Order order) {
        long id = insertReturningId(
                "INSERT INTO orders (customer_id, product_id, quantity, total) VALUES (?, ?, ?, ?) RETURNING id",
                order.getCustomerId(), order.getProductId(), order.getQuantity(), order.getTotal());
        order.setId(id);
        return order;
    }

    /**
     * Builds the WHERE clause from the non-null filters. Only fixed column names are concatenated,
     * the values are always bound as parameters and appended to args in placeholder order.
     */
    private static String where(Long customerId, Long productId, List<Object> args) {
        StringBuilder where = new StringBuilder();
        if (customerId != null) {
            where.append(" WHERE customer_id = ?");
            args.add(customerId);
        }
        if (productId != null) {
            where.append(customerId == null ? " WHERE" : " AND").append(" product_id = ?");
            args.add(productId);
        }
        return where.toString();
    }
}
