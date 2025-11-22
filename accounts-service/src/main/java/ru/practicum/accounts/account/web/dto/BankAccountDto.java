package ru.practicum.accounts.account.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BankAccountDto(
        UUID id,
        String accountNumber,
        String currency,
        BigDecimal balance
) { }
