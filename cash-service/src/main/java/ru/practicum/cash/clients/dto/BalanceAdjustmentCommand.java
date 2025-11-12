package ru.practicum.cash.clients.dto;

import java.math.BigDecimal;

import ru.practicum.cash.model.OperationType;

public record BalanceAdjustmentCommand(
        BigDecimal amount,
        OperationType type
) {
}
