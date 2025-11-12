package ru.practicum.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.transfer.model.TransferEntity;

import java.util.UUID;

/**
 * Репозиторий переводов.
 */
public interface TransferRepository extends JpaRepository<TransferEntity, UUID> {
}
