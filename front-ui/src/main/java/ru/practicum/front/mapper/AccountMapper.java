package ru.practicum.front.mapper;

import ru.practicum.front.service.Dto;
import ru.practicum.front.service.dto.AccountResponse;

/**
 * Маппинг ответов backend в DTO для UI.
 */
public interface AccountMapper {

    Dto.UserProfile toUserProfile(AccountResponse response);

    Dto.UserShort toUserShort(AccountResponse response);
}
