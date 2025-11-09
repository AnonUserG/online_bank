package ru.practicum.bank.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class GatewayApiClient {

    private static final Logger log = LoggerFactory.getLogger(GatewayApiClient.class);

    private final RestClient client;

    public GatewayApiClient(@Value("${app.gateway-base-url}") String gatewayBaseUrl) {
        this.client = RestClient.builder().baseUrl(gatewayBaseUrl).build();
    }

    // ============== Accounts ==============

    public Map<String, Object> getUserProfile(String login, String bearer) {
        return safeCall(
                () -> client.get()
                        .uri("/api/accounts/users/{login}", login)
                        .header("Authorization", "Bearer " + bearer)
                        .retrieve()
                        .body(Map.class),
                Map.of(
                        "login", login,
                        "name", login,
                        "birthdate", LocalDate.now().minusYears(18).toString(),
                        "status", "stub",
                        "message", "Сервис аккаунтов недоступен"
                ),
                "getUserProfile"
        );
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllUsers(String bearer) {
        return safeCall(
                () -> client.get()
                        .uri("/api/accounts/users")
                        .header("Authorization", "Bearer " + bearer)
                        .retrieve()
                        .body(List.class),
                Collections.emptyList(),
                "getAllUsers"
        );
    }

    public List<String> changePassword(String login, String newPassword, String bearer) {
        var payload = Map.of("login", login, "password", newPassword);
        return safeCall(
                () -> client.post()
                        .uri("/api/accounts/users/{login}/password", login)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + bearer)
                        .body(payload)
                        .retrieve()
                        .body(List.class),
                List.of("Сервис аккаунтов временно недоступен. Попробуйте позже."),
                "changePassword"
        );
    }

    public List<String> updateProfile(String login, String name, String birthdateIso, String bearer) {
        var payload = Map.of("name", name, "birthdate", birthdateIso);
        return safeCall(
                () -> client.patch()
                        .uri("/api/accounts/users/{login}", login)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + bearer)
                        .body(payload)
                        .retrieve()
                        .body(List.class),
                List.of("Сервис аккаунтов временно недоступен. Попробуйте позже."),
                "updateProfile"
        );
    }

    public List<String> register(String login, String password, String name, String birthdateIso) {
        var payload = Map.of("login", login, "password", password, "name", name, "birthdate", birthdateIso);
        return safeCall(
                () -> client.post()
                        .uri("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(List.class),
                List.of("Регистрация временно недоступна. Повторите попытку позже."),
                "register"
        );
    }

    // ============== Cash ==============

    public List<String> cash(String login, String action, String amount, String bearer) {
        var payload = Map.of("login", login, "action", action, "value", amount);
        return safeCall(
                () -> client.post()
                        .uri("/api/cash/operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + bearer)
                        .body(payload)
                        .retrieve()
                        .body(List.class),
                List.of("Сервис пополнения и снятия недоступен. Попробуйте позже."),
                "cash"
        );
    }

    // ============== Transfer ==============

    public List<String> transfer(String fromLogin, String toLogin, String amount, String bearer) {
        var payload = Map.of("from_login", fromLogin, "to_login", toLogin, "value", amount);
        return safeCall(
                () -> client.post()
                        .uri("/api/transfer/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + bearer)
                        .body(payload)
                        .retrieve()
                        .body(List.class),
                List.of("Сервис переводов недоступен. Попробуйте позже."),
                "transfer"
        );
    }

    private <T> T safeCall(Supplier<T> call, T fallback, String operation) {
        try {
            return call.get();
        } catch (RestClientException ex) {
            log.warn("Gateway call '{}' failed: {}", operation, ex.getMessage());
            return fallback;
        }
    }
}
