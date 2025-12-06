package ru.practicum.transfer.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.transfer.notifications.NotificationEvent;

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

    public void sendTransferOut(String login, String receiver, BigDecimal amount, String currency) {
        send(login, "TRANSFER_OUT",
                "Вы перевели %s %s пользователю %s".formatted(amount, currency, receiver));
    }

    public void sendTransferIn(String login, String sender, BigDecimal amount, String currency) {
        send(login, "TRANSFER_IN",
                "На ваш счёт поступило %s %s от %s".formatted(amount, currency, sender));
    }

    private void send(String login, String type, String message) {
        if (!enabled) {
            return;
        }
        NotificationEvent event = NotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(login)
                .type(type)
                .title(message)
                .content(message)
                .createdAt(OffsetDateTime.now())
                .build();
        kafkaTemplate.send(notificationsTopic, login, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Failed to send notification event type={} userId={}: {}", type, login, ex.getMessage());
                    }
                });
    }
}
