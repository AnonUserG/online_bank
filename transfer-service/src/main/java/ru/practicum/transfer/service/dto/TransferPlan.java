package ru.practicum.transfer.service.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Внутренний план перевода перед сохранением.
 */
public record TransferPlan(
        UUID fromAccountId,
        UUID toAccountId,
        BigDecimal amount,
        String currency,
        String idempotencyKey
) {
}
