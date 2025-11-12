package ru.practicum.accounts.account.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountDetailsDto(
        UUID userId,
        UUID bankAccountId,
        String login,
        String accountNumber,
        String currency,
        BigDecimal balance
) {
}
