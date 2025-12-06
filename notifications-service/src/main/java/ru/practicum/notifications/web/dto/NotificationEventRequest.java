package ru.practicum.notifications.web.dto;

import jakarta.validation.constraints.NotBlank;
import ru.practicum.notifications.kafka.NotificationEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * REST-запрос для ручной отправки уведомления (используется в основном для тестирования).
 */
public record NotificationEventRequest(
        @NotBlank(message = "userId is required")
        String userId,
        @NotBlank(message = "type is required")
        String type,
        @NotBlank(message = "title is required")
        String title,
        @NotBlank(message = "content is required")
        String content
) {
    public NotificationEvent toEvent() {
        return NotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
