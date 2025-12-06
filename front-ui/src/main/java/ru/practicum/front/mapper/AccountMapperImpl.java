package ru.practicum.front.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.front.service.Dto;
import ru.practicum.front.service.dto.AccountResponse;

/**
 * Ручная реализация маппера вместо MapStruct для стабильной сборки.
 */
@Component
public class AccountMapperImpl implements AccountMapper {

    @Override
    public Dto.UserProfile toUserProfile(AccountResponse response) {
        if (response == null) {
            return null;
        }
        return new Dto.UserProfile(
                response.login(),
                response.name(),
                response.birthdate()
        );
    }

    @Override
    public Dto.UserShort toUserShort(AccountResponse response) {
        if (response == null) {
            return null;
        }
        return new Dto.UserShort(
                response.login(),
                response.name()
        );
    }
}
