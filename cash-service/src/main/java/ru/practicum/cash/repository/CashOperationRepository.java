package ru.practicum.cash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.cash.model.CashOperationEntity;

import java.util.UUID;

/**
 * Репозиторий операций наличных.
 */
public interface CashOperationRepository extends JpaRepository<CashOperationEntity, UUID> {
}
