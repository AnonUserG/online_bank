package ru.practicum.bank.account.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.practicum.bank.account.model.AccountEntity;
import ru.practicum.bank.account.model.BankAccountEntity;
import ru.practicum.bank.account.repository.AccountRepository;
import ru.practicum.bank.account.web.dto.ChangePasswordRequest;
import ru.practicum.bank.account.web.dto.RegisterAccountRequest;
import ru.practicum.bank.account.web.dto.UpdateAccountRequest;
import ru.practicum.bank.clients.KeycloakAdminClient;
import ru.practicum.bank.clients.NotificationsClient;
import ru.practicum.bank.exception.AccountAlreadyExistsException;
import ru.practicum.bank.exception.AccountDeletionException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private AccountRepository repository;
    private KeycloakAdminClient keycloakAdminClient;
    private NotificationsClient notificationsClient;
    private AccountService service;

    @BeforeEach
    void setUp() {
        repository = mock(AccountRepository.class);
        keycloakAdminClient = mock(KeycloakAdminClient.class);
        notificationsClient = mock(NotificationsClient.class);
        service = new AccountService(repository, new AccountMapper(), keycloakAdminClient, notificationsClient);
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
        AccountEntity entity = new AccountEntity();
        entity.setLogin("alice");
        entity.setBirthdate(LocalDate.of(1990, 1, 1));
        entity.setName("Alice");
        when(repository.findByLogin("alice")).thenReturn(Optional.of(entity));

        var response = service.changePassword("alice", new ChangePasswordRequest("newpass123"));

        assertThat(response).isEmpty();
        verify(keycloakAdminClient).resetPassword("alice", "newpass123");
        verify(notificationsClient).sendPasswordChanged("alice");
    }

    @Test
    void updateProfileUpdatesEntity() {
        AccountEntity entity = new AccountEntity();
        entity.setLogin("alice");
        entity.setBirthdate(LocalDate.of(1990, 1, 1));
        entity.setName("Alice");
        when(repository.findByLogin("alice")).thenReturn(Optional.of(entity));

        var response = service.updateProfile("alice", new UpdateAccountRequest("Alice Cooper", LocalDate.of(1991, 2, 2)));

        assertThat(response).isEmpty();
        assertThat(entity.getName()).isEqualTo("Alice Cooper");
        verify(repository).save(entity);
        verify(notificationsClient).sendProfileUpdated("alice");
    }

    @Test
    void deleteAccountFailsWhenBalancePositive() {
        AccountEntity entity = new AccountEntity();
        entity.setLogin("alice");
        entity.setBirthdate(LocalDate.of(1990, 1, 1));
        entity.setName("Alice");
        BankAccountEntity bank = new BankAccountEntity();
        bank.setBalance(BigDecimal.TEN);
        entity.setBankAccount(bank);
        when(repository.findByLogin("alice")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.deleteAccount("alice"))
                .isInstanceOf(AccountDeletionException.class)
                .hasMessageContaining("Нельзя удалить аккаунт");

        verifyNoInteractions(keycloakAdminClient);
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteAccountRemovesEntityWhenBalanceZero() {
        AccountEntity entity = new AccountEntity();
        entity.setLogin("alice");
        entity.setBirthdate(LocalDate.of(1990, 1, 1));
        entity.setName("Alice");
        BankAccountEntity bank = new BankAccountEntity();
        bank.setBalance(BigDecimal.ZERO);
        entity.setBankAccount(bank);
        when(repository.findByLogin("alice")).thenReturn(Optional.of(entity));

        var result = service.deleteAccount("alice");

        assertThat(result).isEmpty();
        verify(keycloakAdminClient).deleteUser("alice");
        verify(repository).delete(entity);
        verify(notificationsClient).sendAccountDeleted("alice");
    }
}

