package ru.practicum.front.service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BankAccountResponse(
        UUID id,
        String accountNumber,
        String currency,
        BigDecimal balance
) { }
