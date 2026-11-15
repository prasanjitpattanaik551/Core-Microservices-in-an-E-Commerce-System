package com.microservice.ecommerce.kafka;

import com.microservice.common.constants.KafkaTopics;
import com.microservice.common.event.OrderCreatedEvent;
import com.microservice.common.event.OrderStatusUpdatedEvent;
import com.microservice.ecommerce.model.Orders;
import com.microservice.ecommerce.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for order-related events.
 * Publishes events when orders are created or their status changes.
 */
@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes an OrderCreatedEvent to Kafka when a new order is placed.
     * The payment-service consumes this event to initiate payment processing.
     */
    public void publishOrderCreated(Orders order, User user, Map<Long, Integer> productQuantities) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getOrderDate(),
                productQuantities
        );

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaTopics.ORDER_CREATED, order.getId().toString(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ OrderCreatedEvent sent successfully for orderId={}, partition={}, offset={}",
                        order.getId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("❌ Failed to send OrderCreatedEvent for orderId={}: {}",
                        order.getId(), ex.getMessage());
            }
        });
    }

    /**
     * Publishes an OrderStatusUpdatedEvent when order status changes
     * (e.g., PENDING -> PAID, PENDING -> PAYMENT_FAILED).
     */
    public void publishOrderStatusUpdated(Long orderId, String previousStatus, String newStatus) {
        OrderStatusUpdatedEvent event = new OrderStatusUpdatedEvent(
                orderId, previousStatus, newStatus, "Status updated after payment processing"
        );

        kafkaTemplate.send(KafkaTopics.ORDER_STATUS_UPDATED, orderId.toString(), event);
        log.info("📤 OrderStatusUpdatedEvent sent: orderId={}, {} -> {}", orderId, previousStatus, newStatus);
    }
}
