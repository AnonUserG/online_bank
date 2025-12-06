package ru.practicum.accounts.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import ru.practicum.accounts.account.model.AccountEntity;
import ru.practicum.accounts.account.model.BankAccountEntity;
import ru.practicum.accounts.account.repository.AccountRepository;
import ru.practicum.accounts.account.repository.BankAccountRepository;
import ru.practicum.accounts.account.web.dto.BalanceAdjustmentRequest;
import ru.practicum.accounts.account.web.dto.BalanceOperationType;
import ru.practicum.accounts.account.web.dto.ChangePasswordRequest;
import ru.practicum.accounts.account.web.dto.RegisterAccountRequest;
import ru.practicum.accounts.account.web.dto.UpdateAccountRequest;
import ru.practicum.accounts.clients.KeycloakAdminClient;
import ru.practicum.accounts.clients.NotificationsClient;
import ru.practicum.accounts.exception.AccountAlreadyExistsException;
import ru.practicum.accounts.exception.AccountDeletionException;
import ru.practicum.accounts.exception.InsufficientFundsException;

class AccountServiceTest {

    private AccountRepository repository;
    private BankAccountRepository bankAccountRepository;
    private KeycloakAdminClient keycloakAdminClient;
    private NotificationsClient notificationsClient;
    private AccountService service;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(AccountRepository.class);
        bankAccountRepository = org.mockito.Mockito.mock(BankAccountRepository.class);
        keycloakAdminClient = org.mockito.Mockito.mock(KeycloakAdminClient.class);
        notificationsClient = org.mockito.Mockito.mock(NotificationsClient.class);
        service = new AccountService(repository, bankAccountRepository, new AccountMapperImpl(), keycloakAdminClient, notificationsClient);
    }

    @Test
    void registerCreatesAccountAndCallsKeycloak() {
        RegisterAccountRequest request = new RegisterAccountRequest("alice", "pwd123", "Alice", null, LocalDate.of(1990, 1, 1));
        when(repository.existsByLogin("alice")).thenReturn(false);
        when(keycloakAdminClient.createUser(any(), any(), any(), any())).thenReturn("kc-id");
        when(repository.save(any(AccountEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.register(request);

        assertThat(response).isEmpty();
        verify(keycloakAdminClient).createUser("alice", "pwd123", "Alice", null);
        verify(notificationsClient).sendRegistrationCompleted("alice");
        ArgumentCaptor<AccountEntity> captor = ArgumentCaptor.forClass(AccountEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getLogin()).isEqualTo("alice");
        assertThat(captor.getValue().getBankAccount()).isNotNull();
    }

    @Test
    void registerFailsWhenLoginExists() {
        RegisterAccountRequest request = new RegisterAccountRequest("alice", "pwd123", "Alice", null, LocalDate.of(1990, 1, 1));
        when(repository.existsByLogin("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(AccountAlreadyExistsException.class);

        verifyNoInteractions(keycloakAdminClient);
    }

    @Test
    void changePasswordDelegatesToKeycloak() {
        AccountEntity entity = baseAccount();
        when(repository.findByLogin("alice")).thenReturn(Optional.of(entity));

        var response = service.changePassword("alice", new ChangePasswordRequest("newpass123"));

        assertThat(response).isEmpty();
        verify(keycloakAdminClient).resetPassword("alice", "newpass123");
        verify(notificationsClient).sendPasswordChanged("alice");
    }

    @Test
    void updateProfileUpdatesEntity() {
        AccountEntity entity = baseAccount();
        when(repository.findByLogin("alice")).thenReturn(Optional.of(entity));

        var response = service.updateProfile("alice", new UpdateAccountRequest("Alice Cooper", LocalDate.of(1991, 2, 2)));

        assertThat(response).isEmpty();
        assertThat(entity.getName()).isEqualTo("Alice Cooper");
        verify(repository).save(entity);
        verify(notificationsClient).sendProfileUpdated("alice");
    }

    @Test
    void deleteAccountFailsWhenBalancePositive() {
        AccountEntity entity = baseAccount();
        BankAccountEntity bank = bank(BigDecimal.TEN);
        entity.setBankAccount(bank);
        when(repository.findByLogin("alice")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.deleteAccount("alice"))
                .isInstanceOf(AccountDeletionException.class);

        verifyNoInteractions(keycloakAdminClient);
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteAccountRemovesEntityWhenBalanceZero() {
        AccountEntity entity = baseAccount();
        BankAccountEntity bank = bank(BigDecimal.ZERO);
        entity.setBankAccount(bank);
        when(repository.findByLogin("alice")).thenReturn(Optional.of(entity));

        var result = service.deleteAccount("alice");

        assertThat(result).isEmpty();
        verify(keycloakAdminClient).deleteUser("alice");
        verify(repository).delete(entity);
        verify(notificationsClient).sendAccountDeleted("alice");
    }

    @Test
    void getAccountDetailsReturnsBankInfo() {
        AccountEntity entity = baseAccount();
        BankAccountEntity bank = bank(BigDecimal.valueOf(150));
        entity.setBankAccount(bank);
        when(repository.findByLogin("alice")).thenReturn(Optional.of(entity));

        var details = service.getAccountDetails("alice");

        assertThat(details.login()).isEqualTo("alice");
        assertThat(details.balance()).isEqualByComparingTo("150");
        assertThat(details.currency()).isEqualTo("RUB");
    }

    @Test
    void adjustBalanceDepositsMoney() {
        AccountEntity entity = baseAccount();
        BankAccountEntity bank = bank(BigDecimal.valueOf(100));
        bank.setUser(entity);
        when(bankAccountRepository.findByUserLoginForUpdate("alice")).thenReturn(Optional.of(bank));

        var details = service.adjustBalance("alice",
                new BalanceAdjustmentRequest(BigDecimal.valueOf(50), BalanceOperationType.DEPOSIT, null));

        assertThat(details.balance()).isEqualByComparingTo("150");
        verify(bankAccountRepository).save(bank);
    }

    @Test
    void adjustBalanceFailsOnInsufficientFunds() {
        AccountEntity entity = baseAccount();
        BankAccountEntity bank = bank(BigDecimal.valueOf(30));
        bank.setUser(entity);
        when(bankAccountRepository.findByUserLoginForUpdate("alice")).thenReturn(Optional.of(bank));

        assertThatThrownBy(() -> service.adjustBalance("alice",
                new BalanceAdjustmentRequest(BigDecimal.valueOf(50), BalanceOperationType.WITHDRAW, null)))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds on source account");

        verify(bankAccountRepository, never()).save(any());
    }

    private AccountEntity baseAccount() {
        AccountEntity entity = new AccountEntity();
        entity.setLogin("alice");
        entity.setBirthdate(LocalDate.of(1990, 1, 1));
        entity.setName("Alice");
        return entity;
    }

    private BankAccountEntity bank(BigDecimal balance) {
        BankAccountEntity bank = new BankAccountEntity();
        bank.setBalance(balance);
        bank.setAccountNumber("12345678901234567890");
        bank.setCurrency("RUB");
        return bank;
    }
}


