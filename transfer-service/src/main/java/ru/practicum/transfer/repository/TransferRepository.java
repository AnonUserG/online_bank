package ru.practicum.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.transfer.model.TransferEntity;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<TransferEntity, UUID> {
    Optional<TransferEntity> findByIdempotencyKey(String idempotencyKey);
}
