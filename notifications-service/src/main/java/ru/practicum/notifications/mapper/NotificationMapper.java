package ru.practicum.notifications.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.notifications.kafka.NotificationEvent;
import ru.practicum.notifications.model.NotificationMessage;

/**
 * Конвертация запросов в доменную модель уведомлений.
 */
@Component
public class NotificationMapper {

    public NotificationMessage toMessage(NotificationEvent event) {
        if (event == null) {
            return null;
        }
        var createdAt = event.createdAt() == null ? java.time.OffsetDateTime.now() : event.createdAt();
        return NotificationMessage.builder()
                .eventId(event.eventId())
                .userId(event.userId())
                .type(event.type())
                .title(event.title())
                .content(event.content())
                .createdAt(createdAt)
                .build();
    }
}
