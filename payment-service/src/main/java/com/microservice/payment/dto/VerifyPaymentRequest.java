package com.microservice.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** Body sent by the frontend from Razorpay Checkout's success handler. */
public record VerifyPaymentRequest(
        @JsonProperty("razorpay_order_id")
        @NotBlank(message = "razorpay_order_id is required")
        String razorpayOrderId,

        @JsonProperty("razorpay_payment_id")
        @NotBlank(message = "razorpay_payment_id is required")
        String razorpayPaymentId,

        @JsonProperty("razorpay_signature")
        @NotBlank(message = "razorpay_signature is required")
        String razorpaySignature) {}
