package ru.practicum.exchange.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exchange.model.ExchangeRateEntity;
import ru.practicum.exchange.repository.ExchangeRateRepository;
import ru.practicum.exchange.web.dto.ConvertResponse;
import ru.practicum.exchange.web.dto.RateRequest;
import ru.practicum.exchange.web.dto.RateResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final ExchangeRateRepository repository;

    @Value("${app.base-currency:RUB}")
    String baseCurrency;

    @Transactional(readOnly = true)
    public List<RateResponse> getAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<RateResponse> saveRates(List<RateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        Instant now = Instant.now();
        return requests.stream()
                .map(req -> upsert(req, now))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConvertResponse convert(String from, String to, BigDecimal amount) {
        if (from.equalsIgnoreCase(to)) {
            return new ConvertResponse(from.toUpperCase(), to.toUpperCase(), amount, amount, BigDecimal.ONE);
        }

        BigDecimal baseRateFrom = getMidRate(from);
        BigDecimal baseRateTo = getMidRate(to);

        // amount in base currency
        BigDecimal inBase = from.equalsIgnoreCase(baseCurrency)
                ? amount
                : amount.multiply(baseRateFrom);
        BigDecimal result = to.equalsIgnoreCase(baseCurrency)
                ? inBase
                : inBase.divide(baseRateTo, 4, RoundingMode.HALF_UP);

        BigDecimal effectiveRate = from.equalsIgnoreCase(baseCurrency)
                ? BigDecimal.ONE.divide(baseRateTo, 6, RoundingMode.HALF_UP)
                : baseRateFrom;

        return new ConvertResponse(from.toUpperCase(), to.toUpperCase(), amount, result, effectiveRate);
    }

    private BigDecimal getMidRate(String currency) {
        if (currency.equalsIgnoreCase(baseCurrency)) {
            return BigDecimal.ONE;
        }
        var rate = repository.findByBaseCurrencyIgnoreCaseAndCurrencyIgnoreCase(baseCurrency, currency)
                .orElseThrow(() -> new IllegalArgumentException("Rate not found for currency " + currency));
        return rate.getBuyRate().add(rate.getSellRate())
                .divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
    }

    private ExchangeRateEntity upsert(RateRequest req, Instant updatedAtDefault) {
        String base = req.baseCurrency().toUpperCase(Locale.ROOT);
        String currency = req.currency().toUpperCase(Locale.ROOT);
        var entity = repository.findByBaseCurrencyIgnoreCaseAndCurrencyIgnoreCase(base, currency)
                .orElseGet(ExchangeRateEntity::new);
        entity.setBaseCurrency(base);
        entity.setCurrency(currency);
        entity.setBuyRate(req.buyRate());
        entity.setSellRate(req.sellRate());
        entity.setUpdatedAt(req.generatedAt() == null ? updatedAtDefault : req.generatedAt());
        return repository.save(entity);
    }

    private RateResponse toResponse(ExchangeRateEntity entity) {
        return new RateResponse(
                entity.getBaseCurrency(),
                entity.getCurrency(),
                entity.getBuyRate(),
                entity.getSellRate(),
                entity.getUpdatedAt()
        );
    }
}
