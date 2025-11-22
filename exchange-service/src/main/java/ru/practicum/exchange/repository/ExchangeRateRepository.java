package ru.practicum.exchange.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.exchange.model.ExchangeRateEntity;

import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, Long> {

    Optional<ExchangeRateEntity> findByBaseCurrencyIgnoreCaseAndCurrencyIgnoreCase(String baseCurrency, String currency);
}
