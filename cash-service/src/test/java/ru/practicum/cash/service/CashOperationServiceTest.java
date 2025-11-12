package ru.practicum.cash.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.practicum.cash.clients.AccountsClient;
import ru.practicum.cash.clients.NotificationsClient;
import ru.practicum.cash.clients.dto.AccountDetails;
import ru.practicum.cash.clients.dto.BalanceAdjustmentCommand;
import ru.practicum.cash.model.CashOperationEntity;
import ru.practicum.cash.model.OperationStatus;
import ru.practicum.cash.model.OperationType;
import ru.practicum.cash.repository.CashOperationRepository;
import ru.practicum.cash.web.dto.CashAction;
import ru.practicum.cash.web.dto.CashOperationRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CashOperationServiceTest {

    private CashOperationRepository repository;
    private AccountsClient accountsClient;
    private NotificationsClient notificationsClient;
    private CashOperationService service;

    @BeforeEach
    void setUp() {
        repository = mock(CashOperationRepository.class);
        accountsClient = mock(AccountsClient.class);
        notificationsClient = mock(NotificationsClient.class);
        service = new CashOperationService(repository, accountsClient, notificationsClient);
    }

    @Test
    void depositSuccessSavesOperation() {
        AccountDetails details = new AccountDetails(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "alice",
                "4080",
                "RUB",
                BigDecimal.valueOf(100)
        );
        when(accountsClient.getAccountDetails("alice")).thenReturn(details);
        when(accountsClient.adjustBalance(eq("alice"), any(BalanceAdjustmentCommand.class))).thenReturn(details);
        when(repository.save(any(CashOperationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = service.process(new CashOperationRequest("alice", CashAction.PUT, BigDecimal.valueOf(50)));

        assertThat(result).isEmpty();
        ArgumentCaptor<BalanceAdjustmentCommand> captor = ArgumentCaptor.forClass(BalanceAdjustmentCommand.class);
        verify(accountsClient).adjustBalance(eq("alice"), captor.capture());
        assertThat(captor.getValue().amount()).isEqualByComparingTo("50.00");
        assertThat(captor.getValue().type()).isEqualTo(OperationType.DEPOSIT);
        verify(notificationsClient).sendCashEvent("alice", OperationType.DEPOSIT, new BigDecimal("50.00"), "RUB");
    }

    @Test
    void withdrawSuccessUpdatesStatusAndNotifies() {
        var accountId = UUID.randomUUID();
        AccountDetails current = new AccountDetails(
                UUID.randomUUID(),
                accountId,
                "alice",
                "4080",
                "RUB",
                BigDecimal.valueOf(200)
        );
        AccountDetails updated = new AccountDetails(
                current.userId(),
                current.bankAccountId(),
                current.login(),
                current.accountNumber(),
                current.currency(),
                BigDecimal.valueOf(150)
        );
        when(accountsClient.getAccountDetails("alice")).thenReturn(current);
        when(repository.save(any(CashOperationEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountsClient.adjustBalance(eq("alice"), any(BalanceAdjustmentCommand.class))).thenReturn(updated);

        var result = service.process(new CashOperationRequest("alice", CashAction.GET, BigDecimal.valueOf(50)));

        assertThat(result).isEmpty();
        ArgumentCaptor<BalanceAdjustmentCommand> commandCaptor = ArgumentCaptor.forClass(BalanceAdjustmentCommand.class);
        verify(accountsClient).adjustBalance(eq("alice"), commandCaptor.capture());
        assertThat(commandCaptor.getValue().type()).isEqualTo(OperationType.WITHDRAW);
        assertThat(commandCaptor.getValue().amount()).isEqualByComparingTo("50.00");
        ArgumentCaptor<CashOperationEntity> saveCaptor = ArgumentCaptor.forClass(CashOperationEntity.class);
        verify(repository, atLeast(2)).save(saveCaptor.capture());
        var lastSaved = saveCaptor.getAllValues().get(saveCaptor.getAllValues().size() - 1);
        assertThat(lastSaved.getStatus()).isEqualTo(OperationStatus.DONE);
        verify(notificationsClient).sendCashEvent("alice", OperationType.WITHDRAW, new BigDecimal("50.00"), "RUB");
    }

    @Test
    void withdrawFailsWhenNotEnoughMoney() {
        AccountDetails details = new AccountDetails(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "alice",
                "4080",
                "RUB",
                BigDecimal.valueOf(10)
        );
        when(accountsClient.getAccountDetails("alice")).thenReturn(details);

        var result = service.process(new CashOperationRequest("alice", CashAction.GET, BigDecimal.valueOf(50)));

        assertThat(result).contains("Недостаточно средств на счёте");
        verify(accountsClient, never()).adjustBalance(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void marksOperationFailedWhenAccountsError() {
        AccountDetails details = new AccountDetails(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "alice",
                "4080",
                "RUB",
                BigDecimal.valueOf(100)
        );
        when(accountsClient.getAccountDetails("alice")).thenReturn(details);
        when(repository.save(any(CashOperationEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(accountsClient.adjustBalance(eq("alice"), any(BalanceAdjustmentCommand.class)))
                .thenThrow(new AccountsClient.AccountsClientException("boom", new RuntimeException()));

        var result = service.process(new CashOperationRequest("alice", CashAction.PUT, BigDecimal.valueOf(10)));

        assertThat(result).contains("boom");
        ArgumentCaptor<CashOperationEntity> captor = ArgumentCaptor.forClass(CashOperationEntity.class);
        verify(repository, atLeast(2)).save(captor.capture());
        List<CashOperationEntity> saved = captor.getAllValues();
        assertThat(saved.get(saved.size() - 1).getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    void returnsErrorWhenAccountNotFound() {
        when(accountsClient.getAccountDetails("missing")).thenReturn(null);

        var result = service.process(new CashOperationRequest("missing", CashAction.PUT, BigDecimal.ONE));

        assertThat(result).hasSize(1);
        verify(repository, never()).save(any());
        verifyNoInteractions(notificationsClient);
    }
}
