package ru.practicum.cash.notifications;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record NotificationEvent(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("userId") String userId,
        @JsonProperty("type") String type,
        @JsonProperty("title") String title,
        @JsonProperty("content") String content,
        @JsonProperty("createdAt") OffsetDateTime createdAt
) {
}
