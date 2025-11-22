package ru.practicum.exchange.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record RateRequest(
        @NotBlank String baseCurrency,
        @NotBlank String currency,
        @NotNull BigDecimal buyRate,
        @NotNull BigDecimal sellRate,
        Instant generatedAt
) { }
