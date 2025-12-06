package ru.practicum.notifications.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ru.practicum.notifications.kafka.NotificationEvent;
import ru.practicum.notifications.model.NotificationMessage;

class NotificationMapperTest {

    private final NotificationMapper mapper = new NotificationMapper();

    @Test
    void toMessageCopiesFieldsAndGeneratesIds() {
        NotificationEvent event = NotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .userId("bob")
                .type("ACCOUNT")
                .title("Hello")
                .content("Sample")
                .createdAt(OffsetDateTime.now())
                .build();

        NotificationMessage message = mapper.toMessage(event);

        assertThat(message.getEventId()).isEqualTo(event.eventId());
        assertThat(message.getCreatedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
        assertThat(message.getType()).isEqualTo("ACCOUNT");
        assertThat(message.getUserId()).isEqualTo("bob");
        assertThat(message.getTitle()).isEqualTo("Hello");
        assertThat(message.getContent()).isEqualTo("Sample");
    }
}
