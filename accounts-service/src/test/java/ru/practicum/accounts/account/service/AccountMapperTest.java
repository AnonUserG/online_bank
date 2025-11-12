package ru.practicum.accounts.account.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.practicum.accounts.account.model.AccountEntity;
import ru.practicum.accounts.account.model.BankAccountEntity;

class AccountMapperTest {

    private AccountMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(AccountMapper.class);
    }

    @Test
    void toDtoCopiesUserAndBankFields() {
        AccountEntity entity = accountWithBank();

        var dto = mapper.toDto(entity);

        assertThat(dto.login()).isEqualTo("ivan");
        assertThat(dto.name()).isEqualTo("Ivan Ivanov");
        assertThat(dto.accountNumber()).isEqualTo("40801234123412341234");
        assertThat(dto.currency()).isEqualTo("RUB");
        assertThat(dto.balance()).isEqualByComparingTo("1500.00");
    }

    @Test
    void toDetailsDtoContainsIdentifiers() {
        AccountEntity entity = accountWithBank();

        var dto = mapper.toDetailsDto(entity);

        assertThat(dto.userId()).isEqualTo(entity.getId());
        assertThat(dto.bankAccountId()).isEqualTo(entity.getBankAccount().getId());
        assertThat(dto.balance()).isEqualByComparingTo(entity.getBankAccount().getBalance());
    }

    private AccountEntity accountWithBank() {
        AccountEntity account = new AccountEntity();
        account.setId(UUID.randomUUID());
        account.setLogin("ivan");
        account.setName("Ivan Ivanov");
        account.setBirthdate(LocalDate.of(1990, 5, 20));

        BankAccountEntity bank = new BankAccountEntity();
        bank.setId(UUID.randomUUID());
        bank.setAccountNumber("40801234123412341234");
        bank.setCurrency("RUB");
        bank.setBalance(new BigDecimal("1500.00"));
        account.setBankAccount(bank);
        return account;
    }
}
