package ru.practicum.accounts.account.service;

import java.math.BigDecimal;
import ru.practicum.accounts.account.model.AccountEntity;
import ru.practicum.accounts.account.model.BankAccountEntity;
import ru.practicum.accounts.account.web.dto.AccountDetailsDto;
import ru.practicum.accounts.account.web.dto.AccountDto;
import ru.practicum.accounts.account.web.dto.BankAccountDto;

/**
 * Maps account entities to DTOs.
 */
public interface AccountMapper {

    AccountDto toDto(AccountEntity entity);

    AccountDetailsDto toDetailsDto(AccountEntity entity);

    BigDecimal balanceOrZero(BigDecimal value);

    BankAccountDto toBankDto(BankAccountEntity entity);

    BankAccountEntity primary(AccountEntity entity);

    String primaryAccountNumber(AccountEntity entity);

    String primaryAccountCurrency(AccountEntity entity);

    BigDecimal primaryAccountBalance(AccountEntity entity);

    java.util.UUID primaryAccountId(AccountEntity entity);
}

