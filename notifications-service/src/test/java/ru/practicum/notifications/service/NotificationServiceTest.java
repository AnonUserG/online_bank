package ru.practicum.notifications.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.practicum.notifications.mapper.NotificationMapper;
import ru.practicum.notifications.model.NotificationMessage;
import ru.practicum.notifications.web.dto.NotificationEventRequest;

class NotificationServiceTest {

    @Mock
    private NotificationMapper mapper;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new NotificationService(mapper);
    }

    @Test
    void acceptDelegatesToMapper() {
        NotificationEventRequest request = new NotificationEventRequest("TYPE", "user", "text");
        NotificationMessage message = NotificationMessage.builder()
                .id(UUID.randomUUID())
                .type("TYPE")
                .recipient("user")
                .message("text")
                .createdAt(OffsetDateTime.now())
                .build();
        when(mapper.toMessage(request)).thenReturn(message);

        assertThatCode(() -> service.accept(request)).doesNotThrowAnyException();
        verify(mapper).toMessage(request);
    }
}
