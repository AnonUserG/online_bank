package ru.practicum.accounts.account.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import ru.practicum.accounts.account.model.AccountEntity;
import ru.practicum.accounts.account.model.BankAccountEntity;
import ru.practicum.accounts.account.web.dto.AccountDetailsDto;
import ru.practicum.accounts.account.web.dto.AccountDto;
import ru.practicum.accounts.account.web.dto.BankAccountDto;

/**
 * Manual mapper implementation to avoid MapStruct generation issues.
 */
@Component
public class AccountMapperImpl implements AccountMapper {

    @Override
    public AccountDto toDto(AccountEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AccountDto(
                entity.getLogin(),
                entity.getName(),
                entity.getBirthdate(),
                primaryAccountNumber(entity),
                primaryAccountCurrency(entity),
                primaryAccountBalance(entity)
        );
    }

    @Override
    public AccountDetailsDto toDetailsDto(AccountEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AccountDetailsDto(
                entity.getId(),
                primaryAccountId(entity),
                entity.getLogin(),
                primaryAccountNumber(entity),
                primaryAccountCurrency(entity),
                primaryAccountBalance(entity)
        );
    }

    @Override
    public BigDecimal balanceOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    @Override
    public BankAccountDto toBankDto(BankAccountEntity entity) {
        if (entity == null) {
            return null;
        }
        return new BankAccountDto(entity.getId(), entity.getAccountNumber(), entity.getCurrency(), balanceOrZero(entity.getBalance()));
    }

    @Override
    public BankAccountEntity primary(AccountEntity entity) {
        if (entity == null || entity.getBankAccounts() == null || entity.getBankAccounts().isEmpty()) {
            return null;
        }
        return entity.getBankAccounts().getFirst();
    }

    @Override
    public String primaryAccountNumber(AccountEntity entity) {
        var acc = primary(entity);
        return acc == null ? null : acc.getAccountNumber();
    }

    @Override
    public String primaryAccountCurrency(AccountEntity entity) {
        var acc = primary(entity);
        return acc == null ? null : acc.getCurrency();
    }

    @Override
    public BigDecimal primaryAccountBalance(AccountEntity entity) {
        var acc = primary(entity);
        return acc == null ? BigDecimal.ZERO : balanceOrZero(acc.getBalance());
    }

    @Override
    public java.util.UUID primaryAccountId(AccountEntity entity) {
        var acc = primary(entity);
        return acc == null ? null : acc.getId();
    }
}
