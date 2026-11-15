package com.microservice.payment.service;

import com.microservice.common.event.PaymentProcessedEvent;
import com.microservice.payment.entity.PaymentOrder;
import com.microservice.payment.kafka.PaymentEventProducer;
import com.microservice.payment.repo.PaymentRepo;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Payment flow (Razorpay):
 * 1. OrderCreatedEvent (Kafka) -> createPaymentForOrder(): creates a Razorpay order, status = CREATED
 * 2. Frontend opens Razorpay Checkout using the Razorpay order id
 * 3. Frontend calls /verify -> verifyAndComplete(): checks HMAC signature,
 *    marks SUCCESS / FAILED and publishes PaymentProcessedEvent to Kafka
 *
 * Field mapping on PaymentOrder:
 *   orderId   = Razorpay order id   (order_xxx)
 *   paymentId = Razorpay payment id (pay_xxx)
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepo paymentRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private PaymentEventProducer paymentEventProducer;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    /**
     * Step 1: called from the Kafka consumer. Creates a Razorpay order and stores it as CREATED.
     * Idempotent: if a payment already exists for this ecommerce order, it is returned as-is,
     * so a redelivered Kafka message will not create a duplicate Razorpay order.
     */
    @Transactional
    public PaymentOrder createPaymentForOrder(Long ecommerceOrderId, String userName,
                                              String userEmail, double amount) throws RazorpayException {
        PaymentOrder existing = paymentRepo.findByEcommerceOrderId(ecommerceOrderId);
        if (existing != null) {
            log.warn("Payment already exists for ecommerce orderId={}, skipping duplicate", ecommerceOrderId);
            return existing;
        }

        log.info("Creating Razorpay order for ecommerce orderId={}, amount={}", ecommerceOrderId, amount);

        JSONObject options = new JSONObject();
        options.put("amount", Math.round(amount * 100)); // Razorpay expects paise
        options.put("currency", "INR");
        options.put("receipt", "ecom_" + ecommerceOrderId);

        Order razorpayOrder = razorpayClient.orders.create(options);
        String razorpayOrderId = razorpayOrder.get("id");

        PaymentOrder payment = new PaymentOrder();
        payment.setName(userName);
        payment.setEmail(userEmail);
        payment.setAmount(amount);
        payment.setEcommerceOrderId(ecommerceOrderId);
        payment.setOrderId(razorpayOrderId);
        payment.setStatus("CREATED");
        payment.setCourseName("E-Commerce Order #" + ecommerceOrderId);
        payment.setCreatedAt(LocalDateTime.now());

        PaymentOrder saved = paymentRepo.save(payment);
        log.info("Razorpay order created: razorpayOrderId={}, ecommerceOrderId={}", razorpayOrderId, ecommerceOrderId);
        return saved;
    }

    /** Used by the frontend to get what it needs to open Razorpay Checkout. */
    public PaymentOrder getPaymentByEcommerceOrderId(Long ecommerceOrderId) {
        PaymentOrder payment = paymentRepo.findByEcommerceOrderId(ecommerceOrderId);
        if (payment == null) {
            throw new RuntimeException("Payment not found for order: " + ecommerceOrderId);
        }
        return payment;
    }

    public String getKeyId() {
        return keyId; // public key, safe to send to the frontend
    }

    /**
     * Step 3: verify Razorpay signature, update DB and publish the result to Kafka.
     * The signature is HMAC_SHA256(razorpay_order_id + "|" + razorpay_payment_id, key_secret).
     */
    @Transactional
    public PaymentOrder verifyAndComplete(String razorpayOrderId, String razorpayPaymentId,
                                          String razorpaySignature) {
        PaymentOrder payment = paymentRepo.findByOrderId(razorpayOrderId);
        if (payment == null) {
            throw new RuntimeException("Payment not found for Razorpay order: " + razorpayOrderId);
        }

        // Idempotency: already finished, do not process or publish twice
        if ("SUCCESS".equals(payment.getStatus())) {
            log.warn("Payment already SUCCESS for razorpayOrderId={}", razorpayOrderId);
            return payment;
        }

        boolean valid;
        try {
            JSONObject attrs = new JSONObject();
            attrs.put("razorpay_order_id", razorpayOrderId);
            attrs.put("razorpay_payment_id", razorpayPaymentId);
            attrs.put("razorpay_signature", razorpaySignature);
            valid = Utils.verifyPaymentSignature(attrs, keySecret);
        } catch (RazorpayException e) {
            log.error("Signature verification error for razorpayOrderId={}: {}", razorpayOrderId, e.getMessage());
            valid = false;
        }

        payment.setPaymentId(razorpayPaymentId);
        payment.setStatus(valid ? "SUCCESS" : "FAILED");
        PaymentOrder saved = paymentRepo.save(payment);

        if (valid) {
            log.info("Payment verified: razorpayPaymentId={}, ecommerceOrderId={}",
                    razorpayPaymentId, saved.getEcommerceOrderId());
            emailService.sendPaymentConfirmationFromKafka(
                    saved.getEmail(), saved.getName(), saved.getAmount(), saved.getEcommerceOrderId());
        } else {
            log.warn("Invalid signature for razorpayOrderId={}", razorpayOrderId);
        }

        publishResult(saved);
        return saved;
    }

    /** Called when the customer closes checkout or the payment fails on the frontend. */
    @Transactional
    public PaymentOrder markFailed(String razorpayOrderId) {
        PaymentOrder payment = paymentRepo.findByOrderId(razorpayOrderId);
        if (payment == null) {
            throw new RuntimeException("Payment not found for Razorpay order: " + razorpayOrderId);
        }
        if ("SUCCESS".equals(payment.getStatus())) {
            return payment; // never downgrade a successful payment
        }
        payment.setStatus("FAILED");
        PaymentOrder saved = paymentRepo.save(payment);
        publishResult(saved);
        return saved;
    }

    private void publishResult(PaymentOrder payment) {
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                payment.getEcommerceOrderId(),
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getEmail(),
                payment.getName(),
                LocalDateTime.now()
        );
        paymentEventProducer.publishPaymentProcessed(event);
    }
}