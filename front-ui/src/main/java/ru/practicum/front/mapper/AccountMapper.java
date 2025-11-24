package ru.practicum.front.mapper;

import org.mapstruct.Mapper;
import ru.practicum.front.service.Dto;
import ru.practicum.front.service.dto.AccountResponse;

/** Преобразование ответов backend-сервисов в view-DTO. */
@Mapper(componentModel = "spring")
public interface AccountMapper {

    Dto.UserProfile toUserProfile(AccountResponse response);

    Dto.UserShort toUserShort(AccountResponse response);
}
