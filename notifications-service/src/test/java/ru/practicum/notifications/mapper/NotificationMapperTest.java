package ru.practicum.notifications.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import ru.practicum.notifications.model.NotificationMessage;
import ru.practicum.notifications.web.dto.NotificationEventRequest;

class NotificationMapperTest {

    private final NotificationMapper mapper = new NotificationMapper();

    @Test
    void toMessageCopiesFieldsAndGeneratesIds() {
        NotificationEventRequest request = new NotificationEventRequest("ACCOUNT", "bob", "Hello");

        NotificationMessage message = mapper.toMessage(request);

        assertThat(message.getId()).isNotNull();
        assertThat(message.getCreatedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
        assertThat(message.getType()).isEqualTo("ACCOUNT");
        assertThat(message.getRecipient()).isEqualTo("bob");
        assertThat(message.getMessage()).isEqualTo("Hello");
    }
}
