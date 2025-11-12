package ru.practicum.cash.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.cash.service.CashOperationService;
import ru.practicum.cash.web.dto.CashOperationRequest;

import java.util.List;

@RestController
@RequestMapping("/api/cash")
public class CashOperationController {

    private final CashOperationService cashOperationService;

    public CashOperationController(CashOperationService cashOperationService) {
        this.cashOperationService = cashOperationService;
    }

    @PostMapping("/operations")
    public ResponseEntity<List<String>> operate(@RequestBody @Valid CashOperationRequest request) {
        var errors = cashOperationService.process(request);
        return ResponseEntity.ok(errors);
    }
}
