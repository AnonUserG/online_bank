package ru.practicum.notifications.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.notifications.service.NotificationService;

/**
 * Kafka consumer for notifications topic (at-least-once).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaListener {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${app.kafka.notifications-topic:notifications}",
            containerFactory = "notificationKafkaListenerContainerFactory",
            groupId = "${spring.kafka.consumer.group-id:notifications-service}"
    )
    public void listen(NotificationEvent event) {
        log.debug("Received notification event from Kafka: {}", event);
        notificationService.accept(event);
    }
}
