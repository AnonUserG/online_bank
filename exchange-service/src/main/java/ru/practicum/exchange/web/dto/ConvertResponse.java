package ru.practicum.exchange.web.dto;

import java.math.BigDecimal;

public record ConvertResponse(
        String fromCurrency,
        String toCurrency,
        BigDecimal amount,
        BigDecimal convertedAmount,
        BigDecimal rateUsed
) { }
