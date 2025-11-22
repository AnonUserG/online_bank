package ru.practicum.transfer.clients.dto;

public record BlockCheckResponse(
        boolean allowed,
        String reason
) { }
