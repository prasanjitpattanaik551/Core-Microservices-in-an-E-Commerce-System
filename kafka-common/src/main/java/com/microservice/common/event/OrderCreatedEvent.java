package com.microservice.common.event;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Event published by ecommerce-service when a new order is placed.
 * Consumed by payment-service to initiate payment processing.
 */
public class OrderCreatedEvent implements Serializable {

    private Long orderId;
    private Long userId;
    private String userName;
    private String userEmail;
    private double totalAmount;
    private String status;
    private Date orderDate;
    private Map<Long, Integer> productQuantities;

    public OrderCreatedEvent() {}

    public OrderCreatedEvent(Long orderId, Long userId, String userName, String userEmail,
                             double totalAmount, String status, Date orderDate,
                             Map<Long, Integer> productQuantities) {
        this.orderId = orderId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.totalAmount = totalAmount;
        this.status = status;
        this.orderDate = orderDate;
        this.productQuantities = productQuantities;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }
    public Map<Long, Integer> getProductQuantities() { return productQuantities; }
    public void setProductQuantities(Map<Long, Integer> productQuantities) { this.productQuantities = productQuantities; }

    @Override
    public String toString() {
        return "OrderCreatedEvent{orderId=" + orderId + ", userId=" + userId +
               ", totalAmount=" + totalAmount + ", status='" + status + "'}";
    }
}
