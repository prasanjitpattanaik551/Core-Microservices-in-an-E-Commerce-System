package com.microservice.payment.kafka;

import com.microservice.common.constants.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka configuration for the payment-service.
 * Auto-creates the payment-processed topic on startup.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public NewTopic paymentProcessedTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_PROCESSED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
