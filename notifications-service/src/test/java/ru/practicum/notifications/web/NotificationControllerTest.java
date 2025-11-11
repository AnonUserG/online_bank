package ru.practicum.notifications.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.notifications.service.NotificationService;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    NotificationService notificationService;

    @Test
    void acceptsValidEvent() throws Exception {
        Map<String, Object> payload = Map.of(
                "type", "PASSWORD_CHANGED",
                "recipient", "alice",
                "message", "Пароль изменен"
        );

        mockMvc.perform(post("/api/notifications/events")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isAccepted());

        verify(notificationService).accept(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsInvalidEvent() throws Exception {
        mockMvc.perform(post("/api/notifications/events")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(Map.of("type", "", "recipient", "", "message", ""))))
                .andExpect(status().isBadRequest());
    }
}
