package com.microservice.ecommerce.kafka;

import com.microservice.common.constants.KafkaTopics;
import com.microservice.common.event.PaymentProcessedEvent;
import com.microservice.ecommerce.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for PaymentProcessedEvent from payment-service.
 * Updates the order status in the ecommerce database based on payment outcome.
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final OrderService orderService;

    public PaymentEventConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_PROCESSED, groupId = "ecommerce-service-group")
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("📥 Received PaymentProcessedEvent: {}", event);

        try {
            String newStatus;
            if ("SUCCESS".equalsIgnoreCase(event.getStatus())) {
                newStatus = "PAID";
                log.info("✅ Payment successful for orderId={}. Updating order status to PAID.", event.getOrderId());
            } else {
                newStatus = "PAYMENT_FAILED";
                log.warn("⚠️ Payment failed for orderId={}. Updating order status to PAYMENT_FAILED.", event.getOrderId());
            }

            orderService.updateOrderStatus(event.getOrderId(), newStatus);
            log.info("✅ Order status updated for orderId={} to {}", event.getOrderId(), newStatus);

        } catch (Exception e) {
            log.error("❌ Error processing PaymentProcessedEvent for orderId={}: {}",
                    event.getOrderId(), e.getMessage(), e);
        }
    }
}
