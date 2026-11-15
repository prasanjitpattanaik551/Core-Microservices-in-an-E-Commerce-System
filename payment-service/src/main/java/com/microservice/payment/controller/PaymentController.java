package com.microservice.payment.controller;

import com.microservice.payment.entity.PaymentOrder;
import com.microservice.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Frontend calls this after placing an order to get the data needed to open Razorpay Checkout.
     * GET /api/payment/order/{ecommerceOrderId}
     */
    @GetMapping("/order/{ecommerceOrderId}")
    public ResponseEntity<?> getCheckoutDetails(@PathVariable Long ecommerceOrderId) {
        try {
            PaymentOrder payment = paymentService.getPaymentByEcommerceOrderId(ecommerceOrderId);
            return ResponseEntity.ok(Map.of(
                    "keyId", paymentService.getKeyId(),
                    "razorpayOrderId", payment.getOrderId(),
                    "amount", Math.round(payment.getAmount() * 100), // paise
                    "currency", "INR",
                    "status", payment.getStatus(),
                    "name", payment.getName(),
                    "email", payment.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Frontend calls this from Razorpay's success handler.
     * Body: { "razorpay_order_id": "...", "razorpay_payment_id": "...", "razorpay_signature": "..." }
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> body) {
        String orderId = body.get("razorpay_order_id");
        String paymentId = body.get("razorpay_payment_id");
        String signature = body.get("razorpay_signature");

        if (orderId == null || paymentId == null || signature == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing Razorpay fields"));
        }

        try {
            PaymentOrder result = paymentService.verifyAndComplete(orderId, paymentId, signature);
            if ("SUCCESS".equals(result.getStatus())) {
                return ResponseEntity.ok(Map.of("status", "SUCCESS", "paymentId", result.getPaymentId()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "FAILED", "error", "Invalid payment signature"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Verification failed"));
        }
    }

    /**
     * Frontend calls this when checkout is dismissed or the payment fails.
     * POST /api/payment/failed?razorpayOrderId=order_xxx
     */
    @PostMapping("/failed")
    public ResponseEntity<?> paymentFailed(@RequestParam String razorpayOrderId) {
        try {
            PaymentOrder result = paymentService.markFailed(razorpayOrderId);
            return ResponseEntity.ok(Map.of("status", result.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}