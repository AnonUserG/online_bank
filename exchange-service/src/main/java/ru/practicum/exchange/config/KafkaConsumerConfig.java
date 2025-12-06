package ru.practicum.exchange.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import ru.practicum.exchange.web.dto.RateRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration for rate updates.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, RateRequest> rateConsumerFactory(KafkaProperties properties) {
        Map<String, Object> configs = new HashMap<>(properties.buildConsumerProperties(null));
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configs.put(JsonDeserializer.VALUE_DEFAULT_TYPE, RateRequest.class.getName());
        configs.put(JsonDeserializer.TRUSTED_PACKAGES, RateRequest.class.getPackageName());
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RateRequest> rateKafkaListenerContainerFactory(
            ConsumerFactory<String, RateRequest> rateConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, RateRequest> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(rateConsumerFactory);
        factory.setConcurrency(1); // preserve order (single partition)
        return factory;
    }
}
