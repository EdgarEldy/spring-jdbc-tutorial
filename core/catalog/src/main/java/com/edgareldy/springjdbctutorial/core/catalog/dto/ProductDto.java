package com.edgareldy.springjdbctutorial.core.catalog.dto;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Business shape of a product. The unit price is a decimal with two digits.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class ProductDto {

    private Long id;

    private Long categoryId;

    private String productName;

    private BigDecimal unitPrice;

    public ProductDto() {
    }

    public ProductDto(Long id, Long categoryId, String productName, BigDecimal unitPrice) {
        this.id = id;
        this.categoryId = categoryId;
        this.productName = productName;
        this.unitPrice = unitPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProductDto that = (ProductDto) o;
        return Objects.equals(id, that.id) && Objects.equals(categoryId, that.categoryId) && Objects.equals(productName, that.productName) && Objects.equals(unitPrice, that.unitPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, categoryId, productName, unitPrice);
    }

    @Override
    public String toString() {
        return "ProductDto{" +
                "id=" + id +
                ", categoryId=" + categoryId +
                ", productName=" + productName +
                ", unitPrice=" + unitPrice +
                "}";
    }
}
