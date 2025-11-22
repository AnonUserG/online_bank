package ru.practicum.exchangegen.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payload sent to Exchange service.
 */
public record RatePayload(
        @NotBlank String baseCurrency,
        @NotBlank String currency,
        @NotNull BigDecimal buyRate,
        @NotNull BigDecimal sellRate,
        @NotNull Instant generatedAt
) { }
