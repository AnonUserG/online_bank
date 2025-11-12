package ru.practicum.gateway;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewayApplicationTests {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void fallbackEndpointIsPublic() {
        webTestClient.get()
                .uri("/fallback/accounts")
                .exchange()
                .expectStatus().isEqualTo(SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.service").isEqualTo("accounts")
                .jsonPath("$.errors[0]").isNotEmpty();
    }

    @Test
    void protectedRouteRequiresBearerToken() {
        webTestClient.get()
                .uri("/api/accounts/users")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
