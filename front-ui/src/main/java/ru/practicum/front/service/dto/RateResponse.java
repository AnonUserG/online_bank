package ru.practicum.front.service.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RateResponse(
        String baseCurrency,
        String currency,
        BigDecimal buyRate,
        BigDecimal sellRate,
        Instant generatedAt
) { }
