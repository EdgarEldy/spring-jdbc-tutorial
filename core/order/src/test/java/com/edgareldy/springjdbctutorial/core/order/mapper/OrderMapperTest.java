package com.edgareldy.springjdbctutorial.core.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springjdbctutorial.core.order.dto.OrderDto;
import com.edgareldy.springjdbctutorial.core.order.entity.Order;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Tests OrderMapper: entity to dto and dto to entity, with hand-built objects and no mock.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    void _01_ShouldCopyEveryField_WhenEntityBecomesDto() {
        OrderDto dto = mapper.toDto(new Order(1L, 2L, 3L, 4, new BigDecimal("79.96")));

        assertThat(dto).isEqualTo(new OrderDto(1L, 2L, 3L, 4, new BigDecimal("79.96")));
    }

    @Test
    void _02_ShouldCopyEveryField_WhenDtoBecomesEntity() {
        Order order = mapper.toEntity(new OrderDto(null, 2L, 3L, 4, new BigDecimal("79.96")));

        assertThat(order).isEqualTo(new Order(null, 2L, 3L, 4, new BigDecimal("79.96")));
    }

    @Test
    void _03_ShouldReturnNull_WhenTheInputIsNull() {
        assertThat(mapper.toDto(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }
}
