package ru.practicum.notifications.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Входящее событие для уведомления.
 */
public record NotificationEventRequest(
        @NotBlank(message = "type is required")
        String type,
        @NotBlank(message = "recipient is required")
        String recipient,
        @NotBlank(message = "message is required")
        String message
) {
}
