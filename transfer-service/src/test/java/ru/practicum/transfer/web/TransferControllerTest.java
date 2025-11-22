package ru.practicum.transfer.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.transfer.service.TransferService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransferControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    TransferService transferService;

    @Test
    void returnsServiceResponse() throws Exception {
        when(transferService.process(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        Map<String, Object> payload = Map.of(
                "fromLogin", "alice",
                "toLogin", "bob",
                "value", new BigDecimal("100.00")
        );

        mockMvc.perform(post("/api/transfer/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(payload)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void validatesPayload() throws Exception {
        mockMvc.perform(post("/api/transfer/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of())))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"from_login is required\",\"to_login is required\",\"value is required\"]", false));
    }
}
