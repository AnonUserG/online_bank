package ru.practicum.transfer.clients.dto;

import java.math.BigDecimal;

public record BlockCheckRequest(
        String fromLogin,
        String toLogin,
        String currency,
        BigDecimal amount
) { }
