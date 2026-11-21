package com.microservice.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_order")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PaymentOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String description;
    private Double amount;
    private String orderId;
    private String paymentId;
    private String status;
    private LocalDateTime createdAt;

    /** Reference to the ecommerce order ID that triggered this payment */
    @Column(unique = true)
    private Long ecommerceOrderId;
}
