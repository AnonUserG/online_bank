package ru.practicum.transfer.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.practicum.transfer.clients.dto.AccountDetails;
import ru.practicum.transfer.clients.dto.BalanceAdjustmentCommand;

import java.time.Duration;
import java.util.List;

/**
 * HTTP-клиент сервиса аккаунтов.
 */
@Component
@Slf4j
public class AccountsClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AccountsClient(@Value("${accounts.base-url:http://accounts-service:8082}") String baseUrl,
                          @Value("${accounts.connect-timeout:500}") long connectTimeoutMs,
                          @Value("${accounts.read-timeout:2000}") long readTimeoutMs,
                          RestClient.Builder restClientBuilder) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = restClientBuilder
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .build();
    }

    public AccountDetails getAccountDetails(String login) {
        try {
            return restClient.get()
                    .uri("/api/accounts/internal/users/{login}", login)
                    .retrieve()
                    .body(AccountDetails.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new AccountsClientException("Пользователь '%s' не найден".formatted(login), ex);
        } catch (HttpClientErrorException ex) {
            throw new AccountsClientException(extractErrorMessage(ex), ex);
        } catch (RestClientException ex) {
            log.error("Accounts service unavailable: {}", ex.getMessage());
            throw new AccountsClientException("Сервис аккаунтов недоступен", ex);
        }
    }

    public AccountDetails adjustBalance(String login, BalanceAdjustmentCommand command) {
        try {
            return restClient.post()
                    .uri("/api/accounts/internal/users/{login}/balance", login)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .retrieve()
                    .body(AccountDetails.class);
        } catch (HttpClientErrorException ex) {
            throw new AccountsClientException(extractErrorMessage(ex), ex);
        } catch (RestClientException ex) {
            log.error("Accounts service unavailable: {}", ex.getMessage());
            throw new AccountsClientException("Сервис аккаунтов недоступен", ex);
        }
    }

    private String extractErrorMessage(HttpClientErrorException ex) {
        var payload = ex.getResponseBodyAsString();
        if (payload == null || payload.isBlank()) {
            return "Ошибка сервиса аккаунтов";
        }
        try {
            List<String> errors = objectMapper.readValue(
                    payload,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            if (!errors.isEmpty()) {
                return errors.getFirst();
            }
        } catch (JsonProcessingException ignored) {
            return payload;
        }
        return payload;
    }

    public static class AccountsClientException extends RuntimeException {
        public AccountsClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
