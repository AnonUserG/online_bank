package ru.practicum.exchangegen.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.exchangegen.config.ExchangeGeneratorProperties;
import ru.practicum.exchangegen.model.RatePayload;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Random;

/**
 * Generates simple pseudo-random FX rates around a baseline.
 */
@Service
@RequiredArgsConstructor
public class RateGeneratorService {

    private final ExchangeGeneratorProperties properties;
    private final Random random = new SecureRandom();

    public List<RatePayload> generateRates() {
        String base = properties.getBaseCurrency();
        Instant now = Instant.now();
        return properties.getTargetCurrencies().stream()
                .filter(currency -> !currency.equalsIgnoreCase(base))
                .map(currency -> new RatePayload(
                        base.toUpperCase(),
                        currency.toUpperCase(),
                        randomRate(),
                        randomRate().add(BigDecimal.valueOf(0.05)),
                        now
                ))
                .toList();
    }

    private BigDecimal randomRate() {
        // Range 0.1 .. 150 with 4 decimals
        double value = 0.1 + (150 - 0.1) * random.nextDouble();
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }
}
