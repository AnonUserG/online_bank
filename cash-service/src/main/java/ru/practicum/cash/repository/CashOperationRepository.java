package ru.practicum.cash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.cash.model.CashOperationEntity;

import java.util.Optional;
import java.util.UUID;

public interface CashOperationRepository extends JpaRepository<CashOperationEntity, UUID> {

    Optional<CashOperationEntity> findByIdempotencyKey(String idempotencyKey);
}
