package ru.practicum.bank.account.service;

import org.springframework.stereotype.Component;
import ru.practicum.bank.account.model.AccountEntity;
import ru.practicum.bank.account.model.BankAccountEntity;
import ru.practicum.bank.account.web.dto.AccountDto;

import java.math.BigDecimal;

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
}

