package ru.practicum.transfer.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.transfer.model.TransferEntity;
import ru.practicum.transfer.service.dto.TransferPlan;

/**
 * MapStruct-мэппер для сущности перевода.
 */
@Mapper(componentModel = "spring")
public interface TransferMapper {

    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    TransferEntity toEntity(TransferPlan plan);
}
