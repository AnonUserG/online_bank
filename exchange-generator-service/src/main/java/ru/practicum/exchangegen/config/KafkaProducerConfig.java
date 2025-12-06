package ru.practicum.exchangegen.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import ru.practicum.exchangegen.model.RatePayload;

import java.util.HashMap;
import java.util.Map;

/**
 * Dedicated producer configuration for RatePayload messages.
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, RatePayload> rateProducerFactory(org.springframework.boot.autoconfigure.kafka.KafkaProperties properties) {
        Map<String, Object> configs = new HashMap<>(properties.buildProducerProperties(null));
        // Ensure ordering with single in-flight request; at-most-once by avoiding retries/acks=0 by config.
        configs.putIfAbsent(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configs);
    }

    @Bean
    public KafkaTemplate<String, RatePayload> rateKafkaTemplate(ProducerFactory<String, RatePayload> rateProducerFactory) {
        return new KafkaTemplate<>(rateProducerFactory);
    }
}
