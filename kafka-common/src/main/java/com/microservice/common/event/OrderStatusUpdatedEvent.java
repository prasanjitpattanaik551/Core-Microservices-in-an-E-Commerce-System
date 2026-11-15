package com.microservice.common.event;

import java.io.Serializable;

/**
 * Event published by ecommerce-service when order status changes
 * (e.g., after receiving PaymentProcessedEvent).
 */
public class OrderStatusUpdatedEvent implements Serializable {

    private Long orderId;
    private String previousStatus;
    private String newStatus;
    private String reason;

    public OrderStatusUpdatedEvent() {}

    public OrderStatusUpdatedEvent(Long orderId, String previousStatus, String newStatus, String reason) {
        this.orderId = orderId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    @Override
    public String toString() {
        return "OrderStatusUpdatedEvent{orderId=" + orderId +
               ", previousStatus='" + previousStatus + "', newStatus='" + newStatus + "'}";
    }
}
