package ru.practicum.accounts.account.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to adjust an account balance.
 */
public record BalanceAdjustmentRequest(
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "amount must be positive")
        BigDecimal amount,
        @NotNull(message = "type is required")
        BalanceOperationType type,
        UUID bankAccountId
) {
}

