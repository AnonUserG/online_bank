package ru.practicum.exchangegen.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.exchangegen.model.RatePayload;

import java.util.List;

/**
 * Kafka producer for pushing generated rates to Exchange service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RatesKafkaProducer {

    private final KafkaTemplate<String, RatePayload> kafkaTemplate;

    @Value("${app.kafka.rates-topic:fx-rates}")
    private String topic;

    public void sendRates(List<RatePayload> rates) {
        if (rates == null || rates.isEmpty()) {
            return;
        }
        // at-most-once semantics: no retries, best effort fire-and-forget
        for (RatePayload rate : rates) {
            kafkaTemplate.send(topic, rate.currency(), rate)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Failed to send rate {}->{}: {}", rate.baseCurrency(), rate.currency(), ex.getMessage());
                        }
                    });
        }
    }
}
