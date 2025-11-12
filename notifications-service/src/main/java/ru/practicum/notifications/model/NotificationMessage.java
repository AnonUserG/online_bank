package ru.practicum.notifications.model;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Событие, отправляемое пользователю.
 */
@Data
@Builder
public class NotificationMessage {
    private UUID id;
    private String type;
    private String recipient;
    private String message;
    private OffsetDateTime createdAt;
}
