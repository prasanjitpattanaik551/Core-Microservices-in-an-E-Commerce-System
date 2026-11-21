package com.microservice.payment.controller;

import com.microservice.common.security.AuthenticatedUser;
import com.microservice.payment.dto.VerifyPaymentRequest;
import com.microservice.payment.entity.PaymentOrder;
import com.microservice.payment.exception.BadRequestException;
import com.microservice.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

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

    /** Called from Razorpay's success handler. The signature is verified on the server. */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyPayment(@Valid @RequestBody VerifyPaymentRequest body,
                                                             @AuthenticationPrincipal AuthenticatedUser caller) {
        requireOwner(paymentService.getPaymentByRazorpayOrderId(body.razorpayOrderId()), caller, false);

        PaymentOrder result = paymentService.verifyAndComplete(
                body.razorpayOrderId(), body.razorpayPaymentId(), body.razorpaySignature());

        if (!"SUCCESS".equals(result.getStatus())) {
            throw new BadRequestException("Invalid payment signature");
        }
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "paymentId", result.getPaymentId()));
    }

    /** Called when checkout is dismissed or the payment fails. */
    @PostMapping("/failed")
    public ResponseEntity<Map<String, String>> paymentFailed(@RequestParam String razorpayOrderId,
                                                             @AuthenticationPrincipal AuthenticatedUser caller) {
        requireOwner(paymentService.getPaymentByRazorpayOrderId(razorpayOrderId), caller, false);
        PaymentOrder result = paymentService.markFailed(razorpayOrderId);
        return ResponseEntity.ok(Map.of("status", result.getStatus()));
    }

    /** A payment belongs to the user whose email is on it. */
    private void requireOwner(PaymentOrder payment, AuthenticatedUser caller, boolean adminAllowed) {
        boolean owner = caller.getEmail() != null && caller.getEmail().equalsIgnoreCase(payment.getEmail());
        if (!owner && !(adminAllowed && caller.isAdmin())) {
            throw new AccessDeniedException("This payment does not belong to you");
        }
    }
}
