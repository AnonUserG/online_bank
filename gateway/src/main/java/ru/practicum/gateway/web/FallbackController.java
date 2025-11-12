package ru.practicum.gateway.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Fallback-эндпоинты для circuit breaker.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping(value = "/{service}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> fallbackGet(@PathVariable String service) {
        String message = "Service '" + service + "' temporarily unavailable. Please try later.";
        Map<String, Object> body = Map.of(
                "service", service,
                "status", "unavailable",
                "message", message,
                "errors", List.of(message),
                "timestamp", Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @PostMapping(value = "/{service}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> fallbackPost(@PathVariable String service) {
        return fallbackGet(service);
    }
}
