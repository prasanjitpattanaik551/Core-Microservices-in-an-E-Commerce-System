package com.microservice.common.constants;

/**
 * Central place for all Kafka topic names used across microservices.
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // prevent instantiation
    }

    /** E-commerce -> Payment: new order placed, initiate payment */
    public static final String ORDER_CREATED = "order-created-topic";

    /** Payment -> E-commerce: payment result (success/failure) */
    public static final String PAYMENT_PROCESSED = "payment-processed-topic";

    /** E-commerce -> all: order status changed */
    public static final String ORDER_STATUS_UPDATED = "order-status-updated-topic";
}
