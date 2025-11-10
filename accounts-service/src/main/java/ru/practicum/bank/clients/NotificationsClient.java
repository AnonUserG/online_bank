package ru.practicum.bank.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class NotificationsClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationsClient.class);

    private final RestClient restClient;
    private final boolean enabled;

    public NotificationsClient(@Value("${notifications.base-url:http://notifications-service:8084}") String baseUrl,
                               @Value("${notifications.enabled:true}") boolean enabled) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.enabled = enabled;
    }

    public void sendRegistrationCompleted(String login) {
        send("ACCOUNT_REGISTERED", login, "Регистрация аккаунта завершена");
    }

    public void sendProfileUpdated(String login) {
        send("ACCOUNT_UPDATED", login, "Профиль успешно обновлен");
    }

    public void sendPasswordChanged(String login) {
        send("PASSWORD_CHANGED", login, "Пароль обновлен");
    }

    public void sendAccountDeleted(String login) {
        send("ACCOUNT_DELETED", login, "Аккаунт удален");
    }

    private void send(String type, String login, String message) {
        if (!enabled) {
            return;
        }
        try {
            restClient.post()
                    .uri("/api/notifications/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "type", type,
                            "recipient", login,
                            "message", message
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Notifications service unavailable: {}", ex.getMessage());
        }
    }
}

