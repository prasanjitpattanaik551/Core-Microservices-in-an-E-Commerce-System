package com.microservice.payment.kafka;

import com.microservice.common.constants.KafkaTopics;
import com.microservice.common.event.OrderCreatedEvent;
import com.microservice.common.event.PaymentProcessedEvent;
import com.microservice.payment.entity.PaymentOrder;
import com.microservice.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Kafka consumer that listens for OrderCreatedEvent from ecommerce-service.
 * Processes the payment and publishes a PaymentProcessedEvent back.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentEventProducer paymentEventProducer;

    @KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "payment-service-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("📥 Received OrderCreatedEvent: {}", event);

        try {
            // Process the payment
            PaymentOrder paymentOrder = paymentService.processPaymentFromKafka(
                    event.getOrderId(),
                    event.getUserName(),
                    event.getUserEmail(),
                    event.getTotalAmount()
            );

            // Publish PaymentProcessedEvent back to Kafka
            PaymentProcessedEvent paymentEvent = new PaymentProcessedEvent(
                    event.getOrderId(),
                    paymentOrder.getPaymentId(),
                    paymentOrder.getOrderId(),
                    paymentOrder.getStatus(),
                    paymentOrder.getAmount(),
                    paymentOrder.getEmail(),
                    paymentOrder.getName(),
                    LocalDateTime.now()
            );

            paymentEventProducer.publishPaymentProcessed(paymentEvent);
            log.info("✅ Payment processed and event published for ecommerce orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("❌ Error processing payment for orderId={}: {}", event.getOrderId(), e.getMessage(), e);

            // Publish a FAILED event so ecommerce-service knows payment failed
            PaymentProcessedEvent failedEvent = new PaymentProcessedEvent(
                    event.getOrderId(),
                    null,
                    null,
                    "FAILED",
                    event.getTotalAmount(),
                    event.getUserEmail(),
                    event.getUserName(),
                    LocalDateTime.now()
            );
            paymentEventProducer.publishPaymentProcessed(failedEvent);
        }
    }
}
