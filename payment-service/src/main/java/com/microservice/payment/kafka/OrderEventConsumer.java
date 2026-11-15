package com.microservice.payment.kafka;

import com.microservice.common.constants.KafkaTopics;
import com.microservice.common.event.OrderCreatedEvent;
import com.microservice.common.event.PaymentProcessedEvent;
import com.microservice.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Listens for OrderCreatedEvent from ecommerce-service and creates a Razorpay order.
 * The order stays PENDING in ecommerce-service until the customer pays and the
 * /verify endpoint publishes the PaymentProcessedEvent.
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
        log.info("Received OrderCreatedEvent: {}", event);

        try {
            paymentService.createPaymentForOrder(
                    event.getOrderId(),
                    event.getUserName(),
                    event.getUserEmail(),
                    event.getTotalAmount()
            );
            log.info("Razorpay order created, waiting for customer payment. ecommerce orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("Could not create Razorpay order for orderId={}: {}", event.getOrderId(), e.getMessage(), e);

            // Tell ecommerce-service so the order is marked PAYMENT_FAILED
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