package ru.practicum.accounts.account.service;

import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.accounts.account.model.AccountEntity;
import ru.practicum.accounts.account.model.BankAccountEntity;
import ru.practicum.accounts.account.web.dto.AccountDetailsDto;
import ru.practicum.accounts.account.web.dto.AccountDto;
import ru.practicum.accounts.account.web.dto.BankAccountDto;

/**
 * Maps account entities to DTOs.
 */
@Mapper(componentModel = "spring")
public interface AccountMapper {

    @Mapping(target = "accountNumber", expression = "java(primaryAccountNumber(entity))")
    @Mapping(target = "currency", expression = "java(primaryAccountCurrency(entity))")
    @Mapping(target = "balance", expression = "java(primaryAccountBalance(entity))")
    AccountDto toDto(AccountEntity entity);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "bankAccountId", expression = "java(primaryAccountId(entity))")
    @Mapping(target = "accountNumber", expression = "java(primaryAccountNumber(entity))")
    @Mapping(target = "currency", expression = "java(primaryAccountCurrency(entity))")
    @Mapping(target = "balance", expression = "java(primaryAccountBalance(entity))")
    AccountDetailsDto toDetailsDto(AccountEntity entity);

    @Named("balanceOrZero")
    default BigDecimal balanceOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    default BankAccountDto toBankDto(BankAccountEntity entity) {
        if (entity == null) {
            return null;
        }
        return new BankAccountDto(entity.getId(), entity.getAccountNumber(), entity.getCurrency(), balanceOrZero(entity.getBalance()));
    }

    default BankAccountEntity primary(AccountEntity entity) {
        if (entity == null || entity.getBankAccounts() == null || entity.getBankAccounts().isEmpty()) {
            return null;
        }
        return entity.getBankAccounts().getFirst();
    }

    default String primaryAccountNumber(AccountEntity entity) {
        var acc = primary(entity);
        return acc == null ? null : acc.getAccountNumber();
    }

    default String primaryAccountCurrency(AccountEntity entity) {
        var acc = primary(entity);
        return acc == null ? null : acc.getCurrency();
    }

    default BigDecimal primaryAccountBalance(AccountEntity entity) {
        var acc = primary(entity);
        return acc == null ? BigDecimal.ZERO : balanceOrZero(acc.getBalance());
    }

    default java.util.UUID primaryAccountId(AccountEntity entity) {
        var acc = primary(entity);
        return acc == null ? null : acc.getId();
    }
}

