package ru.practicum.notifications.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.notifications.kafka.NotificationEvent;
import ru.practicum.notifications.service.NotificationService;
import ru.practicum.notifications.web.dto.NotificationEventRequest;

/**
 * REST-ручки приема уведомлений.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/events")
    public ResponseEntity<Void> publish(@RequestBody @Valid NotificationEventRequest request) {
        NotificationEvent event = request.toEvent();
        notificationService.accept(event);
        return ResponseEntity.accepted().build();
    }
}
