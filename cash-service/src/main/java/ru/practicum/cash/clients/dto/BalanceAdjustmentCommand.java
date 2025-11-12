package ru.practicum.cash.clients.dto;

import ru.practicum.cash.model.OperationType;

import java.math.BigDecimal;

/**
 * Команда на изменение баланса.
 */
public record BalanceAdjustmentCommand(
        BigDecimal amount,
        OperationType type
) {
}
