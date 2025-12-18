package ru.practicum.exchange.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.exchange.service.ExchangeService;
import ru.practicum.exchange.web.dto.RateRequest;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Consumes generated FX rates from Kafka and stores them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RatesKafkaListener {

    private final ExchangeService exchangeService;
    private final MeterRegistry meterRegistry;
    private final AtomicLong lastUpdateEpochSeconds = new AtomicLong();

    @PostConstruct
    void registerGauge() {
        meterRegistry.gauge("exchange_rates_seconds_since_update", lastUpdateEpochSeconds,
                state -> {
                    long last = state.get();
                    if (last == 0) {
                        return -1.0;
                    }
                    return (double) (Instant.now().getEpochSecond() - last);
                });
    }

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
        lastUpdateEpochSeconds.set(Instant.now().getEpochSecond());
        log.debug("Consumed rate {}->{} updated at {}", request.baseCurrency(), request.currency(), request.generatedAt());
    }
}
