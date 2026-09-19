package com.edgareldy.springjdbctutorial.ws.payload.catalog;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Body of the create and update product calls.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class ProductRequest {

    @NotNull
    private Long categoryId;

    @NotBlank
    @Size(max = 150)
    private String productName;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal unitPrice;

    public ProductRequest() {
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
        ProductRequest that = (ProductRequest) o;
        return Objects.equals(categoryId, that.categoryId) && Objects.equals(productName, that.productName)
                && Objects.equals(unitPrice, that.unitPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(categoryId, productName, unitPrice);
    }

    @Override
    public String toString() {
        return "ProductRequest{" +
                "categoryId=" + categoryId +
                ", productName=" + productName +
                ", unitPrice=" + unitPrice +
                "}";
    }
}
