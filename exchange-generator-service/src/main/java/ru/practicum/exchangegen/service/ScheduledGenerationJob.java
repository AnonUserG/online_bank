package ru.practicum.exchangegen.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.practicum.exchangegen.config.ExchangeGeneratorProperties;
import ru.practicum.exchangegen.model.RatePayload;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Periodically generates and pushes rates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledGenerationJob {

    private final RateGeneratorService generatorService;
    private final RatesKafkaProducer ratesKafkaProducer;
    private final ExchangeGeneratorProperties properties;
    private final AtomicReference<List<RatePayload>> lastRates = new AtomicReference<>(List.of());

    @Scheduled(fixedDelayString = "${app.exchange-generator.period-ms:3000}")
    public void generateAndPush() {
        List<RatePayload> rates = generatorService.generateRates();
        lastRates.set(rates);
        log.info("Отправлены обновленные курсы валют, количество записей={}", rates.size());
        ratesKafkaProducer.sendRates(rates);
        log.debug("Generated {} rates and sent to Kafka topic", rates.size());
    }

    /**
     * Exposed for tests.
     */
    public List<RatePayload> getLastRates() {
        return lastRates.get();
    }
}
