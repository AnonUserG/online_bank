package ru.practicum.accounts.account.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Public profile data exposed via REST.
 */
public record AccountDto(
        String login,
        String name,
        LocalDate birthdate,
        String accountNumber,
        String currency,
        BigDecimal balance
) {
}

