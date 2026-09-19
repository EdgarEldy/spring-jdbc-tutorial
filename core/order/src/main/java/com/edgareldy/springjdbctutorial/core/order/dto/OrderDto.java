package com.edgareldy.springjdbctutorial.core.order.dto;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Business shape of an order. The total is computed by the service, never taken from the caller.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class OrderDto {

    private Long id;

    private Long customerId;

    private Long productId;

    private int quantity;

    private BigDecimal total;

    public OrderDto() {
    }

    public OrderDto(Long id, Long customerId, Long productId, int quantity, BigDecimal total) {
        this.id = id;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.total = total;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderDto that = (OrderDto) o;
        return quantity == that.quantity && Objects.equals(id, that.id) && Objects.equals(customerId, that.customerId)
                && Objects.equals(productId, that.productId) && Objects.equals(total, that.total);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customerId, productId, quantity, total);
    }

    @Override
    public String toString() {
        return "OrderDto{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", total=" + total +
                "}";
    }
}
