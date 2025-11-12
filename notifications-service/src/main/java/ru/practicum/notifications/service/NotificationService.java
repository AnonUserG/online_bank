package ru.practicum.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.notifications.mapper.NotificationMapper;
import ru.practicum.notifications.model.NotificationMessage;
import ru.practicum.notifications.web.dto.NotificationEventRequest;

/**
 * Бизнес-логика обработки уведомлений.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationMapper notificationMapper;

    public void accept(NotificationEventRequest request) {
        NotificationMessage message = notificationMapper.toMessage(request);
        log.info("Notification event id={} type={} recipient={} message={}",
                message.getId(),
                message.getType(),
                message.getRecipient(),
                message.getMessage());
        log.debug("Notification processed at {}", message.getCreatedAt());
    }
}
