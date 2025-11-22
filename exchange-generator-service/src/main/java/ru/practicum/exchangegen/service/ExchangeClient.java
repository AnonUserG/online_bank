package ru.practicum.exchangegen.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.practicum.exchangegen.model.RatePayload;

import java.util.List;

/**
 * Lightweight client for pushing generated rates to Exchange service.
 */
@Component
@Slf4j
public class ExchangeClient {

    private final RestClient restClient;

    public ExchangeClient(@Value("${app.exchange-base-url:http://exchange-service:8086}") String exchangeBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(exchangeBaseUrl)
                .build();
    }

    public void sendRates(List<RatePayload> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        try {
            restClient.post()
                    .uri("/api/exchange/rates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Pushed {} generated rates to exchange", payload.size());
        } catch (RestClientException ex) {
            log.warn("Failed to push rates to exchange: {}", ex.getMessage());
        }
    }
}
