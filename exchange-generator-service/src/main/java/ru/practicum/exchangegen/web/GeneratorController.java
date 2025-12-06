package ru.practicum.exchangegen.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.exchangegen.model.RatePayload;
import ru.practicum.exchangegen.service.RateGeneratorService;
import ru.practicum.exchangegen.service.RatesKafkaProducer;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * REST endpoints to inspect and trigger rate generation.
 */
@RestController
@RequestMapping("/api/generator")
@RequiredArgsConstructor
public class GeneratorController {

    private final RateGeneratorService generatorService;
    private final RatesKafkaProducer ratesKafkaProducer;
    private final AtomicReference<List<RatePayload>> lastRates = new AtomicReference<>(List.of());

    @PostMapping("/run")
    public ResponseEntity<List<RatePayload>> runNow() {
        List<RatePayload> rates = generatorService.generateRates();
        lastRates.set(rates);
        ratesKafkaProducer.sendRates(rates);
        return ResponseEntity.ok(rates);
    }

    @GetMapping("/last")
    public ResponseEntity<List<RatePayload>> last() {
        return ResponseEntity.ok(lastRates.get());
    }
}
