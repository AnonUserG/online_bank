package ru.practicum.transfer.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.practicum.transfer.clients.dto.BlockCheckRequest;
import ru.practicum.transfer.clients.dto.BlockCheckResponse;

@Component
@Slf4j
public class BlockerClient {

    private final RestClient client;

    public BlockerClient(@Value("${app.blocker-base-url:http://blocker-service:8088}") String baseUrl) {
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public BlockCheckResponse check(BlockCheckRequest request) {
        try {
            return client.post()
                    .uri("/api/blocker/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(BlockCheckResponse.class);
        } catch (RestClientException ex) {
            log.warn("Blocker unavailable, allow by default: {}", ex.getMessage());
            return new BlockCheckResponse(true, null);
        }
    }
}
