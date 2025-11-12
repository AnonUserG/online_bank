package ru.practicum.accounts.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;

/**
 * HTTP client of the notifications service.
 */
@Component
@Slf4j
public class NotificationsClient {

    private final RestClient restClient;
    private final boolean enabled;

    public NotificationsClient(@Value("${notifications.base-url:http://notifications-service:8084}") String baseUrl,
                               @Value("${notifications.enabled:true}") boolean enabled) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(500));
        factory.setReadTimeout(Duration.ofMillis(1000));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .build();
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



