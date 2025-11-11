package ru.practicum.notifications.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.practicum.notifications.web.dto.NotificationEventRequest;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void accept(NotificationEventRequest request) {
        log.info("Notification event id={} type={} recipient={} message={}",
                UUID.randomUUID(),
                request.type(),
                request.recipient(),
                request.message());

        log.debug("Notification processed at {}", OffsetDateTime.now());
    }
}
