package ru.practicum.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    void fallbackGetBuildsUnavailableResponse() {
        var response = controller.fallbackGet("accounts");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("service")).isEqualTo("accounts");
        assertThat(body.get("errors")).asList().contains("Service 'accounts' temporarily unavailable. Please try later.");
    }

    @Test
    void fallbackPostDelegatesToGet() {
        var response = controller.fallbackPost("transfer");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().get("service")).isEqualTo("transfer");
    }
}
