package com.microservice.payment.kafka;

import com.microservice.common.constants.KafkaTopics;
import com.microservice.common.event.PaymentProcessedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for payment-related events.
 * Publishes PaymentProcessedEvent after payment processing completes.
 */
@Component
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a PaymentProcessedEvent to Kafka.
     * The ecommerce-service consumes this to update order status.
     */
    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        String key = event.getOrderId() != null ? event.getOrderId().toString() : "unknown";

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(KafkaTopics.PAYMENT_PROCESSED, key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ PaymentProcessedEvent sent: orderId={}, status={}, partition={}, offset={}",
                        event.getOrderId(),
                        event.getStatus(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("❌ Failed to send PaymentProcessedEvent for orderId={}: {}",
                        event.getOrderId(), ex.getMessage());
            }
        });
    }
}
