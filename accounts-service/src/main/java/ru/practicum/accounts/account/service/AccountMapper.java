package ru.practicum.accounts.account.service;

import org.springframework.stereotype.Component;
import ru.practicum.accounts.account.model.AccountEntity;
import ru.practicum.accounts.account.model.BankAccountEntity;
import ru.practicum.accounts.account.web.dto.AccountDetailsDto;
import ru.practicum.accounts.account.web.dto.AccountDto;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class AccountMapper {

    public AccountDto toDto(AccountEntity entity) {
        BankAccountEntity bank = entity.getBankAccount();
        BigDecimal balance = bank != null ? bank.getBalance() : BigDecimal.ZERO;
        String accountNumber = bank != null ? bank.getAccountNumber() : null;
        String currency = bank != null ? bank.getCurrency() : null;

        return new AccountDto(
                entity.getLogin(),
                entity.getName(),
                entity.getBirthdate(),
                accountNumber,
                currency,
                balance
        );
    }

    public AccountDetailsDto toDetailsDto(AccountEntity entity) {
        BankAccountEntity bank = entity.getBankAccount();
        UUID bankAccountId = bank != null ? bank.getId() : null;
        String accountNumber = bank != null ? bank.getAccountNumber() : null;
        String currency = bank != null ? bank.getCurrency() : null;
        BigDecimal balance = bank != null ? bank.getBalance() : BigDecimal.ZERO;

        return new AccountDetailsDto(
                entity.getId(),
                bankAccountId,
                entity.getLogin(),
                accountNumber,
                currency,
                balance
        );
    }
}
