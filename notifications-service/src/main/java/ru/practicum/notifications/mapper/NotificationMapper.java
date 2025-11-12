package ru.practicum.notifications.mapper;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;
import ru.practicum.notifications.model.NotificationMessage;
import ru.practicum.notifications.web.dto.NotificationEventRequest;

/**
 * Конвертация запросов в доменную модель уведомлений.
 */
@Component
public class NotificationMapper {

    public NotificationMessage toMessage(NotificationEventRequest request) {
        if (request == null) {
            return null;
        }
        return NotificationMessage.builder()
                .id(UUID.randomUUID())
                .type(request.type())
                .recipient(request.recipient())
                .message(request.message())
                .createdAt(OffsetDateTime.now())
                .build();
    }
}
