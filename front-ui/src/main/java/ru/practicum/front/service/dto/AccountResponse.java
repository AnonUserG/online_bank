package ru.practicum.front.service.dto;

import java.time.LocalDate;

/**
 * Ответ API аккаунтов, используемый фронтом.
 */
public record AccountResponse(
        String login,
        String name,
        LocalDate birthdate
) {
}
