package ru.practicum.cash.clients.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Данные аккаунта из accounts-service.
 */
public record AccountDetails(
        UUID userId,
        UUID bankAccountId,
        String login,
        String accountNumber,
        String currency,
        BigDecimal balance
) {
}
