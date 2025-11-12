package ru.practicum.accounts.account.service;

import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.accounts.account.model.AccountEntity;
import ru.practicum.accounts.account.web.dto.AccountDetailsDto;
import ru.practicum.accounts.account.web.dto.AccountDto;

/**
 * Maps account entities to DTOs.
 */
@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "accountNumber", source = "bankAccount.accountNumber")
    @Mapping(target = "currency", source = "bankAccount.currency")
    @Mapping(target = "balance", source = "bankAccount.balance", qualifiedByName = "balanceOrZero")
    AccountDto toDto(AccountEntity entity);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "bankAccountId", source = "bankAccount.id")
    @Mapping(target = "accountNumber", source = "bankAccount.accountNumber")
    @Mapping(target = "currency", source = "bankAccount.currency")
    @Mapping(target = "balance", source = "bankAccount.balance", qualifiedByName = "balanceOrZero")
    AccountDetailsDto toDetailsDto(AccountEntity entity);

    @Named("balanceOrZero")
    default BigDecimal balanceOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

