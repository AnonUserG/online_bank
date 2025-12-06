package ru.practicum.notifications.service;

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

    public void accept(NotificationEvent event) {
        NotificationMessage message = notificationMapper.toMessage(event);
        log.info("Notification event id={} type={} userId={} title={} content={}",
                message.getEventId(),
                message.getType(),
                message.getUserId(),
                message.getTitle(),
                message.getContent());
        log.debug("Notification processed at {}", message.getCreatedAt());
    }
}
