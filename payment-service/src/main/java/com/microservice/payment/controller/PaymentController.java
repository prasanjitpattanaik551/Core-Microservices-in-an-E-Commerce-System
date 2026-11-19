package com.microservice.payment.controller;

import com.microservice.common.security.AuthenticatedUser;
import com.microservice.payment.entity.PaymentOrder;
import com.microservice.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    /**
     * Data needed to open Razorpay Checkout.
     * GET /api/payment/order/{ecommerceOrderId}
     */
    @GetMapping("/order/{ecommerceOrderId}")
    public Map<String, Object> getCheckoutDetails(@PathVariable Long ecommerceOrderId,
                                                  @AuthenticationPrincipal AuthenticatedUser caller) {
        PaymentOrder payment = paymentService.getPaymentByEcommerceOrderId(ecommerceOrderId);
        requireOwner(payment, caller, true);

        return Map.<String, Object>of(
                "keyId", paymentService.getKeyId(),
                "razorpayOrderId", payment.getOrderId(),
                "amount", Math.round(payment.getAmount() * 100), // paise
                "currency", "INR",
                "status", payment.getStatus(),
                "name", payment.getName(),
                "email", payment.getEmail()
        );
    }

    /**
     * Called from Razorpay's success handler.
     * Body: { "razorpay_order_id": "...", "razorpay_payment_id": "...", "razorpay_signature": "..." }
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal AuthenticatedUser caller) {
        String orderId = body.get("razorpay_order_id");
        String paymentId = body.get("razorpay_payment_id");
        String signature = body.get("razorpay_signature");

        if (orderId == null || paymentId == null || signature == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing Razorpay fields"));
        }

        requireOwner(paymentService.getPaymentByRazorpayOrderId(orderId), caller, false);

        PaymentOrder result = paymentService.verifyAndComplete(orderId, paymentId, signature);
        if ("SUCCESS".equals(result.getStatus())) {
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "paymentId", result.getPaymentId()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("status", "FAILED", "error", "Invalid payment signature"));
    }

    /** Called when checkout is dismissed or the payment fails. */
    @PostMapping("/failed")
    public ResponseEntity<?> paymentFailed(@RequestParam String razorpayOrderId,
                                           @AuthenticationPrincipal AuthenticatedUser caller) {
        requireOwner(paymentService.getPaymentByRazorpayOrderId(razorpayOrderId), caller, false);
        PaymentOrder result = paymentService.markFailed(razorpayOrderId);
        return ResponseEntity.ok(Map.of("status", result.getStatus()));
    }

    /** A payment belongs to the user whose email is on it. */
    private void requireOwner(PaymentOrder payment, AuthenticatedUser caller, boolean adminAllowed) {
        boolean owner = caller.getEmail() != null && caller.getEmail().equalsIgnoreCase(payment.getEmail());
        if (!owner && !(adminAllowed && caller.isAdmin())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This payment does not belong to you");
        }
    }
}