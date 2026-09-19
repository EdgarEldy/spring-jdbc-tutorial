package com.edgareldy.springjdbctutorial.ws.payload.catalog;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A product as returned by the API.
 * <p>
 * Created edgar.muhamyangabo on 9/20/26
 * Author : edgar.muhamyangabo
 * Date : 9/20/26
 * Project : spring-jdbc-tutorial
 */
public class ProductResponse {

    private Long id;

    private Long categoryId;

    private String productName;

    private BigDecimal unitPrice;

    public ProductResponse() {
    }

    public ProductResponse(Long id, Long categoryId, String productName, BigDecimal unitPrice) {
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
        ProductResponse that = (ProductResponse) o;
        return Objects.equals(id, that.id) && Objects.equals(categoryId, that.categoryId) && Objects.equals(productName, that.productName) && Objects.equals(unitPrice, that.unitPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, categoryId, productName, unitPrice);
    }

    @Override
    public String toString() {
        return "ProductResponse{" +
                "id=" + id +
                ", categoryId=" + categoryId +
                ", productName=" + productName +
                ", unitPrice=" + unitPrice +
                "}";
    }
}
