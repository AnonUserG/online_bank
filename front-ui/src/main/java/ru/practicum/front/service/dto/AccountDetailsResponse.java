package ru.practicum.front.service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountDetailsResponse(
        UUID userId,
        UUID bankAccountId,
        String login,
        String accountNumber,
        String currency,
        BigDecimal balance
) { }
