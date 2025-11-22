package ru.practicum.transfer.mapper;

import ru.practicum.transfer.model.TransferEntity;
import ru.practicum.transfer.service.dto.TransferPlan;

/**
 * Mapper for converting transfer plans into persistent entities.
 */
public interface TransferMapper {

    TransferEntity toEntity(TransferPlan plan);
}
