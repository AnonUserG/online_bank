package ru.practicum.front.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.practicum.front.service.dto.AccountDetailsResponse;
import ru.practicum.front.service.dto.AccountResponse;
import ru.practicum.front.service.dto.BankAccountResponse;
import ru.practicum.front.service.dto.RateResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * HTTP-клиент для обращения к backend-сервисам напрямую (без Spring Cloud Gateway).
 */
@Component
@Slf4j
public class GatewayApiClient {

    private final RestClient accountsClient;
    private final RestClient cashClient;
    private final RestClient transferClient;
    private final RestClient exchangeClient;

    public GatewayApiClient(@Value("${app.accounts-base-url}") String accountsBaseUrl,
                            @Value("${app.cash-base-url}") String cashBaseUrl,
                            @Value("${app.transfer-base-url}") String transferBaseUrl,
                            @Value("${app.exchange-base-url}") String exchangeBaseUrl,
                            RestClient.Builder restClientBuilder) {
        this.accountsClient = restClientBuilder.baseUrl(accountsBaseUrl).build();
        this.cashClient = restClientBuilder.baseUrl(cashBaseUrl).build();
        this.transferClient = restClientBuilder.baseUrl(transferBaseUrl).build();
        this.exchangeClient = restClientBuilder.baseUrl(exchangeBaseUrl).build();
    }

    public AccountResponse getUserProfile(String login, String bearer) {
        return safeCall(
                () -> accountsClient.get()
                        .uri("/api/accounts/users/{login}", login)
                        .header("Authorization", "Bearer " + bearer)
                        .retrieve()
                        .body(AccountResponse.class),
                new AccountResponse(login, login, LocalDate.now().minusYears(18)),
                "getUserProfile"
        );
    }

    public List<AccountResponse> getAllUsers(String bearer) {
        return safeCall(
                () -> {
                    AccountResponse[] response = accountsClient.get()
                            .uri("/api/accounts/users")
                            .header("Authorization", "Bearer " + bearer)
                            .retrieve()
                            .body(AccountResponse[].class);
                    return response == null ? List.of() : List.of(response);
                },
                List.of(),
                "getAllUsers"
        );
    }

    public AccountDetailsResponse getAccountDetails(String login, String bearer) {
        return safeCall(
                () -> accountsClient.get()
                        .uri("/api/accounts/internal/users/{login}", login)
                        .header("Authorization", "Bearer " + bearer)
                        .retrieve()
                        .body(AccountDetailsResponse.class),
                null,
                "getAccountDetails"
        );
    }

    public List<String> changePassword(String login, String newPassword, String bearer) {
        var payload = Map.of("login", login, "password", newPassword);
        return safeCall(
                () -> accountsClient.post()
                        .uri("/api/accounts/users/{login}/password", login)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + bearer)
                        .body(payload)
                        .retrieve()
                        .body(List.class),
                List.of("Не удалось изменить пароль."),
                "changePassword"
        );
    }

    public List<String> updateProfile(String login, String name, String birthdateIso, String bearer) {
        var payload = Map.of("name", name, "birthdate", birthdateIso);
        return safeCall(
                () -> accountsClient.patch()
                        .uri("/api/accounts/users/{login}", login)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + bearer)
                        .body(payload)
                        .retrieve()
                        .body(List.class),
                List.of("Не удалось обновить профиль."),
                "updateProfile"
        );
    }

    public List<String> register(String login, String password, String name, String birthdateIso) {
        var payload = Map.of("login", login, "password", password, "name", name, "birthdate", birthdateIso);
        return safeCall(
                () -> accountsClient.post()
                        .uri("/api/accounts/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(List.class),
                List.of("Не удалось зарегистрировать пользователя."),
                "register"
        );
    }

    public List<String> cash(String login, String action, String amount, String bearer) {
        var payload = Map.of("login", login, "action", action, "value", amount);
        return safeCall(
                () -> cashClient.post()
                        .uri("/api/cash/operations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + bearer)
                        .body(payload)
                        .retrieve()
                        .body(List.class),
                List.of("Не удалось выполнить операцию со счётом."),
                "cash"
        );
    }

    public List<String> transfer(String fromLogin, String toLogin, String amount, String bearer) {
        var payload = Map.of("from_login", fromLogin, "to_login", toLogin, "value", amount);
        return safeCall(
                () -> transferClient.post()
                        .uri("/api/transfer/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + bearer)
                        .body(payload)
                        .retrieve()
                        .body(List.class),
                List.of("Не удалось выполнить перевод."),
                "transfer"
        );
    }

    public List<RateResponse> getRates(String bearer) {
        return safeCall(
                () -> {
                    RateResponse[] response = exchangeClient.get()
                            .uri("/api/exchange/rates")
                            .header("Authorization", "Bearer " + bearer)
                            .retrieve()
                            .body(RateResponse[].class);
                    return response == null ? List.of() : List.of(response);
                },
                List.of(),
                "getRates"
        );
    }

    public List<BankAccountResponse> getUserAccounts(String login, String bearer) {
        return safeCall(
                () -> {
                    BankAccountResponse[] response = accountsClient.get()
                            .uri("/api/accounts/internal/users/{login}/accounts", login)
                            .header("Authorization", "Bearer " + bearer)
                            .retrieve()
                            .body(BankAccountResponse[].class);
                    return response == null ? List.of() : List.of(response);
                },
                List.of(),
                "getUserAccounts"
        );
    }

    private <T> T safeCall(Supplier<T> call, T fallback, String operation) {
        try {
            return call.get();
        } catch (RestClientException ex) {
            log.warn("API call '{}' failed: {}", operation, ex.getMessage());
            return fallback;
        }
    }
}
