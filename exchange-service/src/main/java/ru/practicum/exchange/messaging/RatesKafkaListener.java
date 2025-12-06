package ru.practicum.exchange.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.exchange.service.ExchangeService;
import ru.practicum.exchange.web.dto.RateRequest;

import java.util.List;

/**
 * Consumes generated FX rates from Kafka and stores them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RatesKafkaListener {

    private final ExchangeService exchangeService;

    @KafkaListener(
            topics = "${app.kafka.rates-topic:fx-rates}",
            containerFactory = "rateKafkaListenerContainerFactory",
            autoStartup = "true"
    )
    public void consumeRate(RateRequest request) {
        if (request == null) {
            return;
        }
        exchangeService.saveRates(List.of(request));
        log.debug("Consumed rate {}->{} updated at {}", request.baseCurrency(), request.currency(), request.generatedAt());
    }
}
