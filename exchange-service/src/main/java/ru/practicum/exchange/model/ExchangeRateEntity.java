package ru.practicum.exchange.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "exchange_rates")
@Getter
@Setter
public class ExchangeRateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String baseCurrency;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal buyRate;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal sellRate;

    @Column(nullable = false)
    private Instant updatedAt;
}
