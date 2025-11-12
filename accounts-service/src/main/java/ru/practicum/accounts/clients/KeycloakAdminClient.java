package ru.practicum.accounts.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP client that works with Keycloak Admin API.
 */
@Component
@Slf4j
public class KeycloakAdminClient {

    private final RestClient restClient;
    private final String realm;
    private final String adminUsername;
    private final String adminPassword;

    public KeycloakAdminClient(@Value("${keycloak.base-url:http://keycloak:8080}") String baseUrl,
                               @Value("${keycloak.realm:bank}") String realm,
                               @Value("${keycloak.admin.username:admin}") String adminUsername,
                               @Value("${keycloak.admin.password:admin123}") String adminPassword) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.realm = realm;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    public String createUser(String username, String password, String name, String email) {
        String token = adminToken();
        try {
            Map<String, Object> payload = buildCreateUserPayload(username, password, name, email);
            var response = restClient.post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            if (response.getHeaders().getLocation() != null) {
                String path = response.getHeaders().getLocation().getPath();
                return path.substring(path.lastIndexOf('/') + 1);
            }
            return findUserId(username, token);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new IllegalStateException("Keycloak user already exists");
            }
            throw new IllegalStateException("Keycloak call failed: " + ex.getMessage(), ex);
        }
    }

    public void resetPassword(String username, String newPassword) {
        String token = adminToken();
        String userId = findUserId(username, token);
        try {
            restClient.put()
                    .uri("/admin/realms/{realm}/users/{id}/reset-password", realm, userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(Map.of(
                            "type", "password",
                            "value", newPassword,
                            "temporary", false
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to reset password in Keycloak", ex);
        }
    }

    public void deleteUser(String username) {
        String token = adminToken();
        String userId = findUserId(username, token);
        try {
            restClient.delete()
                    .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Unable to delete user in Keycloak", ex);
        }
    }

    private String adminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        TokenResponse tokenResponse = restClient.post()
                .uri("/realms/master/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (tokenResponse == null || tokenResponse.accessToken == null) {
            throw new IllegalStateException("Keycloak admin token is missing");
        }
        return tokenResponse.accessToken;
    }

    private String findUserId(String username, String token) {
        try {
            List<Map<String, Object>> body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/admin/realms/{realm}/users")
                            .queryParam("username", username)
                            .queryParam("exact", true)
                            .build(realm))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve()
                    .body(List.class);

            if (body == null || body.isEmpty()) {
                throw new IllegalStateException("Keycloak user not found");
            }
            Object id = body.get(0).get("id");
            if (id == null) {
                throw new IllegalStateException("Keycloak user id missing");
            }
            return id.toString();
        } catch (RestClientException ex) {
            log.error("Keycloak lookup error: {}", ex.getMessage());
            throw new IllegalStateException("Unable to query Keycloak users", ex);
        }
    }

    private Map<String, Object> buildCreateUserPayload(String username, String password, String name, String email) {
        var payload = new HashMap<String, Object>();
        payload.put("username", username);
        payload.put("enabled", true);
        payload.put("firstName", name);
        if (email != null && !email.isBlank()) {
            payload.put("email", email);
        }
        payload.put("credentials", List.of(Map.of(
                "type", "password",
                "value", password,
                "temporary", false
        )));
        return payload;
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken) {
    }
}



