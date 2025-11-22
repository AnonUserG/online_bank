package ru.practicum.exchangegen.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * External configuration for exchange generator.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.exchange-generator")
public class ExchangeGeneratorProperties {

    /**
     * Base currency that acts as reference (e.g. RUB).
     */
    @NotBlank
    private String baseCurrency = "RUB";

    /**
     * Currencies to generate against the base currency.
     */
    @NotEmpty
    private List<String> targetCurrencies = List.of("USD", "CNY");

    /**
     * Generation period in milliseconds.
     */
    @Min(500)
    private long periodMs = 3000;
}
