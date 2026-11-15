package com.microservice.payment.service;

import com.microservice.payment.entity.PaymentOrder;
import com.microservice.payment.repo.PaymentRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepo paymentRepo;

    @Autowired
    private EmailService emailService;

    private final WebClient webClient;

    public PaymentService(@Value("${nvidia.api.key:}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://integrate.api.nvidia.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String createOrder(PaymentOrder orderDetails) {
        log.info("Creating payment order...");

        // 1. Save order to DB
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        orderDetails.setOrderId(orderId);
        orderDetails.setStatus("CREATED");
        orderDetails.setCreatedAt(LocalDateTime.now());
        paymentRepo.save(orderDetails);

        // 2. Call Nvidia AI to generate a confirmation message
        String prompt = String.format(
                "Generate a short friendly order confirmation message for a customer named %s " +
                        "who just placed an order for Rs %.0f. " +
                        "Keep it under 3 sentences. Include the order ID: %s.",
                orderDetails.getName(),
                orderDetails.getAmount(),
                orderId
        );

        String requestBody = "{"
                + "\"model\": \"meta/llama-3.1-8b-instruct\","
                + "\"messages\": [{\"role\": \"user\", \"content\": \"" + prompt.replace("\"", "\\\"") + "\"}],"
                + "\"max_tokens\": 150"
                + "}";

        try {
            String aiResponse = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            String message = extractContent(aiResponse);
            return orderId + "||" + message;

        } catch (Exception e) {
            log.warn("Nvidia AI call failed: {}", e.getMessage());
            return orderId + "||Order confirmed! Your order is being processed. Order ID: " + orderId;
        }
    }

    // Extract "content" field from Nvidia JSON response
    private String extractContent(String json) {
        try {
            int start = json.indexOf("\"content\":\"") + 11;
            int end = json.indexOf("\"", start);
            return json.substring(start, end).replace("\\n", " ");
        } catch (Exception e) {
            return "Order confirmed successfully!";
        }
    }

    public void updateOrderStatus(String paymentId, String orderId, String status) {
        PaymentOrder order = paymentRepo.findByOrderId(orderId);
        order.setPaymentId(paymentId);
        order.setStatus(status);
        paymentRepo.save(order);

        if ("SUCCESS".equalsIgnoreCase(status)) {
            emailService.sendEmail(
                    order.getEmail(),
                    order.getName(),
                    order.getCourseName(),
                    order.getAmount()
            );
        }
    }

    /**
     * Process payment triggered by Kafka OrderCreatedEvent.
     * Creates a payment record and returns the saved PaymentOrder.
     */
    public PaymentOrder processPaymentFromKafka(Long ecommerceOrderId, String userName,
                                                 String userEmail, double amount) {
        log.info("🔄 Processing Kafka-triggered payment for ecommerce orderId={}, amount={}", ecommerceOrderId, amount);

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setName(userName);
        paymentOrder.setEmail(userEmail);
        paymentOrder.setAmount(amount);
        paymentOrder.setEcommerceOrderId(ecommerceOrderId);
        paymentOrder.setOrderId("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        paymentOrder.setPaymentId("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        paymentOrder.setStatus("SUCCESS");
        paymentOrder.setCreatedAt(LocalDateTime.now());
        paymentOrder.setCourseName("E-Commerce Order #" + ecommerceOrderId);

        PaymentOrder savedOrder = paymentRepo.save(paymentOrder);
        log.info("✅ Payment processed: paymentId={}, orderId={}", savedOrder.getPaymentId(), savedOrder.getOrderId());

        // Send email notification
        emailService.sendPaymentConfirmationFromKafka(userEmail, userName, amount, ecommerceOrderId);

        return savedOrder;
    }
}
