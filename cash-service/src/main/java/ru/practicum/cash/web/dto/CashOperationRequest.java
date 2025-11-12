package ru.practicum.cash.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CashOperationRequest(
        @NotBlank(message = "login is required")
        String login,
        @NotNull(message = "action is required")
        CashAction action,
        @NotNull(message = "value is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "value must be positive")
        BigDecimal value
) {
}
