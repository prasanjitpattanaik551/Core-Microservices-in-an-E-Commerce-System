package com.microservice.ecommerce.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class OrderRequest {

    //key-product id
    //value - quantity
    @NotEmpty(message = "Order must contain at least one product")
    @Size(max = 50, message = "An order can contain at most 50 different products")
    private Map<@NotNull(message = "Product id is required") Long,
            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be at least 1")
            @Max(value = 100, message = "Quantity must be at most 100") Integer> productQuantities;
    // Ignored by the server: the total is always calculated from the product prices in the database.
    private double totalAmount;

    public Map<Long, Integer> getProductQuantities() {
        return productQuantities;
    }

    public void setProductQuantities(Map<Long, Integer> productQuantities) {
        this.productQuantities = productQuantities;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
}
