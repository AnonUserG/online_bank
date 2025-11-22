package ru.practicum.exchange.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.exchange.service.ExchangeService;
import ru.practicum.exchange.web.dto.ConvertResponse;
import ru.practicum.exchange.web.dto.RateRequest;
import ru.practicum.exchange.web.dto.RateResponse;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/exchange")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping("/rates")
    public List<RateResponse> getRates() {
        return exchangeService.getAll();
    }

    @PostMapping("/rates")
    public ResponseEntity<List<RateResponse>> saveRates(@RequestBody @Valid List<RateRequest> requests) {
        return ResponseEntity.ok(exchangeService.saveRates(requests));
    }

    @GetMapping("/convert")
    public ResponseEntity<ConvertResponse> convert(@RequestParam("from") @NotBlank String from,
                                                   @RequestParam("to") @NotBlank String to,
                                                   @RequestParam("amount") @DecimalMin(value = "0.0", inclusive = false) BigDecimal amount) {
        return ResponseEntity.ok(exchangeService.convert(from, to, amount));
    }
}
