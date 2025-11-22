package ru.practicum.exchangegen.service;

import org.junit.jupiter.api.Test;
import ru.practicum.exchangegen.config.ExchangeGeneratorProperties;
import ru.practicum.exchangegen.model.RatePayload;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateGeneratorServiceTest {

    @Test
    void generateRates_returnsEntryPerTargetCurrency() {
        var props = new ExchangeGeneratorProperties();
        props.setBaseCurrency("RUB");
        props.setTargetCurrencies(List.of("USD", "CNY"));

        var service = new RateGeneratorService(props);

        List<RatePayload> rates = service.generateRates();

        assertThat(rates).hasSize(2);
        assertThat(rates)
                .allSatisfy(rate -> {
                    assertThat(rate.baseCurrency()).isEqualTo("RUB");
                    assertThat(rate.currency()).isIn("USD", "CNY");
                    assertThat(rate.buyRate()).isGreaterThan(BigDecimal.ZERO);
                    assertThat(rate.sellRate()).isGreaterThan(BigDecimal.ZERO);
                });
    }
}
