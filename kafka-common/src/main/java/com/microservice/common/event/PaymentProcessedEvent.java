package com.microservice.common.event;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event published by payment-service after payment is processed.
 * Consumed by ecommerce-service to update order status.
 */
public class PaymentProcessedEvent implements Serializable {

    private Long orderId;
    private String paymentId;
    private String paymentOrderId;
    private String status;  // SUCCESS, FAILED
    private double amount;
    private String userEmail;
    private String userName;
    private LocalDateTime processedAt;

    public PaymentProcessedEvent() {}

    public PaymentProcessedEvent(Long orderId, String paymentId, String paymentOrderId,
                                  String status, double amount, String userEmail,
                                  String userName, LocalDateTime processedAt) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.paymentOrderId = paymentOrderId;
        this.status = status;
        this.amount = amount;
        this.userEmail = userEmail;
        this.userName = userName;
        this.processedAt = processedAt;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getPaymentOrderId() { return paymentOrderId; }
    public void setPaymentOrderId(String paymentOrderId) { this.paymentOrderId = paymentOrderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    @Override
    public String toString() {
        return "PaymentProcessedEvent{orderId=" + orderId + ", paymentId='" + paymentId +
               "', status='" + status + "', amount=" + amount + "}";
    }
}
