package ru.practicum.accounts.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.accounts.notifications.NotificationEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Kafka producer of the notifications service (at-least-once).
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

    public void sendRegistrationCompleted(String login) {
        send("ACCOUNT_REGISTERED", login, "Registration completed");
    }

    public void sendProfileUpdated(String login) {
        send("ACCOUNT_UPDATED", login, "Profile updated");
    }

    public void sendPasswordChanged(String login) {
        send("PASSWORD_CHANGED", login, "Password changed");
    }

    public void sendAccountDeleted(String login) {
        send("ACCOUNT_DELETED", login, "Account deleted");
    }

    private void send(String type, String login, String message) {
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

