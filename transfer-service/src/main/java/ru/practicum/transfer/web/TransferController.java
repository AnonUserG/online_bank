package ru.practicum.transfer.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.transfer.service.TransferService;
import ru.practicum.transfer.web.dto.TransferRequest;

import java.util.List;

/**
 * REST-ручки перевода средств.
 */
@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/transactions")
    public ResponseEntity<List<String>> transfer(@RequestBody @Valid TransferRequest request) {
        return ResponseEntity.ok(transferService.process(request));
    }
}
