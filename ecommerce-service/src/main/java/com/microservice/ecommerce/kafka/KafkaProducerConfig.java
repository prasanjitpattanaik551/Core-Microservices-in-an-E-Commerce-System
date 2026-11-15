package com.microservice.ecommerce.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import com.microservice.common.constants.KafkaTopics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka configuration for the ecommerce-service.
 * Auto-creates topics on startup.
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderStatusUpdatedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_STATUS_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
