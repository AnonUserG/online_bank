package ru.practicum.exchange.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RateResponse(
        String baseCurrency,
        String currency,
        BigDecimal buyRate,
        BigDecimal sellRate,
        Instant updatedAt
) { }
