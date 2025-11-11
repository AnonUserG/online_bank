package ru.practicum.notifications.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.notifications.service.NotificationService;
import ru.practicum.notifications.web.dto.NotificationEventRequest;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/events")
    public ResponseEntity<Void> publish(@RequestBody @Valid NotificationEventRequest request) {
        notificationService.accept(request);
        return ResponseEntity.accepted().build();
    }
}
