package ru.practicum.cash.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.cash.model.OperationType;
import ru.practicum.cash.notifications.NotificationEvent;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * HTTP-клиент сервиса уведомлений.
 */
@Component
@Slf4j
public class NotificationsClient {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final String notificationsTopic;
    private final boolean enabled;

    public NotificationsClient(KafkaTemplate<String, NotificationEvent> kafkaTemplate,
                               @Value("${notifications.topic:${KAFKA_NOTIFICATIONS_TOPIC:notifications}}") String notificationsTopic,
                               @Value("${notifications.enabled:true}") boolean enabled) {
        this.kafkaTemplate = kafkaTemplate;
        this.notificationsTopic = notificationsTopic;
        this.enabled = enabled;
    }

    public void sendCashEvent(String login, OperationType type, BigDecimal amount, String currency) {
        if (!enabled) {
            return;
        }
        String message = switch (type) {
            case DEPOSIT -> "На ваш счёт зачислено %s %s".formatted(amount, currency);
            case WITHDRAW -> "Со счёта списано %s %s".formatted(amount, currency);
        };
        String eventType = type == OperationType.DEPOSIT ? "CASH_DEPOSIT" : "CASH_WITHDRAW";
        NotificationEvent event = NotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(login)
                .type(eventType)
                .title(message)
                .content(message)
                .createdAt(OffsetDateTime.now())
                .build();
        kafkaTemplate.send(notificationsTopic, login, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to send notification event type={} userId={}: {}", eventType, login, ex.getMessage());
                    }
                });
    }
}
