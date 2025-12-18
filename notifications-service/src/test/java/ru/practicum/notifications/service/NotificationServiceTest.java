package ru.practicum.notifications.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ru.practicum.notifications.kafka.NotificationEvent;
import ru.practicum.notifications.mapper.NotificationMapper;
import ru.practicum.notifications.model.NotificationMessage;

class NotificationServiceTest {

    @Mock
    private NotificationMapper mapper;

    private NotificationService service;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        meterRegistry = new SimpleMeterRegistry();
        service = new NotificationService(mapper, meterRegistry);
    }

    @Test
    void acceptDelegatesToMapper() {
        NotificationEvent event = NotificationEvent.builder()
                .eventId(java.util.UUID.randomUUID())
                .userId("user")
                .type("TYPE")
                .title("title")
                .content("text")
                .createdAt(OffsetDateTime.now())
                .build();
        NotificationMessage message = NotificationMessage.builder()
                .eventId(event.eventId())
                .type("TYPE")
                .userId("user")
                .title("title")
                .content("text")
                .createdAt(OffsetDateTime.now())
                .build();
        when(mapper.toMessage(event)).thenReturn(message);

        assertThatCode(() -> service.accept(event)).doesNotThrowAnyException();
        verify(mapper).toMessage(event);
    }
}
