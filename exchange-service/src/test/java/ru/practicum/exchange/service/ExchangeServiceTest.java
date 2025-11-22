package ru.practicum.exchange.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.practicum.exchange.model.ExchangeRateEntity;
import ru.practicum.exchange.repository.ExchangeRateRepository;
import ru.practicum.exchange.web.dto.RateRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ExchangeServiceTest {

    private ExchangeRateRepository repository;
    private ExchangeService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ExchangeRateRepository.class);
        service = new ExchangeService(repository);
        service.baseCurrency = "RUB";
    }

    @Test
    void saveRates_upsertsAndReturnsResponses() {
        var req = new RateRequest("RUB", "USD", BigDecimal.valueOf(90), BigDecimal.valueOf(91), Instant.now());
        when(repository.findByBaseCurrencyIgnoreCaseAndCurrencyIgnoreCase("RUB", "USD"))
                .thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.saveRates(List.of(req));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).currency()).isEqualTo("USD");
        assertThat(result.get(0).baseCurrency()).isEqualTo("RUB");
    }

    @Test
    void convert_currencyToBase_usesMidRate() {
        var entity = new ExchangeRateEntity();
        entity.setBaseCurrency("RUB");
        entity.setCurrency("USD");
        entity.setBuyRate(BigDecimal.valueOf(90));
        entity.setSellRate(BigDecimal.valueOf(92));
        entity.setUpdatedAt(Instant.now());

        when(repository.findByBaseCurrencyIgnoreCaseAndCurrencyIgnoreCase("RUB", "USD"))
                .thenReturn(Optional.of(entity));

        var response = service.convert("USD", "RUB", BigDecimal.TEN);

        assertThat(response.convertedAmount()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    void convert_missingRate_throws() {
        when(repository.findByBaseCurrencyIgnoreCaseAndCurrencyIgnoreCase("RUB", "EUR"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.convert("EUR", "RUB", BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
