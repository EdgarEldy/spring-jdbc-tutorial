package com.edgareldy.springjdbctutorial.core.order.dao;

import com.edgareldy.springjdbctutorial.core.order.entity.Order;

import java.util.List;
import java.util.Optional;

/**
 * Data access for orders. Entity types only. Orders are immutable: there is no update and no delete.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public interface OrderDao {

    /** One page of orders ordered by id, each filter is applied only when not null. */
    List<Order> findPage(int page, int size, Long customerId, Long productId);

    /** Number of orders matching the same optional filters. */
    long count(Long customerId, Long productId);

    Optional<Order> findById(Long id);

    /** Inserts the order and returns it with its generated id. */
    Order insert(Order order);
}
