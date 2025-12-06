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
    private UUID eventId;
    private String userId;
    private String type;
    private String title;
    private String content;
    private OffsetDateTime createdAt;
}
