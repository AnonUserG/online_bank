package ru.practicum.transfer.clients.dto;

import java.math.BigDecimal;

import ru.practicum.transfer.model.OperationType;

public record BalanceAdjustmentCommand(
        BigDecimal amount,
        OperationType type
) {
}
