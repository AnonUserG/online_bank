package ru.practicum.notifications.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.notifications.kafka.NotificationEvent;
import ru.practicum.notifications.mapper.NotificationMapper;
import ru.practicum.notifications.model.NotificationMessage;

/**
 * Бизнес-логика обработки уведомлений.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final MeterRegistry meterRegistry;

    public void accept(NotificationEvent event) {
        String user = event != null ? event.userId() : "unknown";
        try {
            NotificationMessage message = notificationMapper.toMessage(event);
            log.info("Notification event id={} type={} userId={} title={} content={}",
                    message.getEventId(),
                    message.getType(),
                    message.getUserId(),
                    message.getTitle(),
                    message.getContent());
            log.debug("Notification processed at {}", message.getCreatedAt());
            meterRegistry.counter("notifications_processed_total",
                    Tags.of("login", message.getUserId(), "outcome", "success")).increment();
        } catch (Exception ex) {
            log.error("Failed to process notification for user={}: {}", user, ex.getMessage(), ex);
            meterRegistry.counter("notifications_processed_total",
                    Tags.of("login", user, "outcome", "failure")).increment();
        }
    }
}
