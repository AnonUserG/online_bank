package ru.practicum.transfer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.practicum.transfer.clients.AccountsClient;
import ru.practicum.transfer.clients.NotificationsClient;
import ru.practicum.transfer.clients.dto.AccountDetails;
import ru.practicum.transfer.clients.dto.BalanceAdjustmentCommand;
import ru.practicum.transfer.mapper.TransferMapper;
import ru.practicum.transfer.mapper.TransferMapperImpl;
import ru.practicum.transfer.model.OperationType;
import ru.practicum.transfer.model.TransferEntity;
import ru.practicum.transfer.model.TransferStatus;
import ru.practicum.transfer.repository.TransferRepository;
import ru.practicum.transfer.web.dto.TransferRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransferServiceTest {

    private TransferRepository repository;
    private AccountsClient accountsClient;
    private NotificationsClient notificationsClient;
    private TransferService service;

    @BeforeEach
    void setUp() {
        repository = mock(TransferRepository.class);
        accountsClient = mock(AccountsClient.class);
        notificationsClient = mock(NotificationsClient.class);
        TransferMapper mapper = new TransferMapperImpl();
        service = new TransferService(repository, accountsClient, notificationsClient, mapper);

        when(repository.save(any(TransferEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void transfersMoneySuccessfully() {
        when(accountsClient.getAccountDetails("alice")).thenReturn(details("alice", BigDecimal.valueOf(500)));
        when(accountsClient.getAccountDetails("bob")).thenReturn(details("bob", BigDecimal.valueOf(100)));

        var result = service.process(new TransferRequest("alice", "bob", BigDecimal.valueOf(150)));

        assertThat(result).isEmpty();
        verify(accountsClient).adjustBalance("alice",
                new BalanceAdjustmentCommand(new BigDecimal("150.00"), OperationType.WITHDRAW));
        verify(accountsClient).adjustBalance("bob",
                new BalanceAdjustmentCommand(new BigDecimal("150.00"), OperationType.DEPOSIT));
        verify(notificationsClient).sendTransferOut("alice", "bob", new BigDecimal("150.00"), "RUB");
        verify(notificationsClient).sendTransferIn("bob", "alice", new BigDecimal("150.00"), "RUB");

        ArgumentCaptor<TransferEntity> captor = ArgumentCaptor.forClass(TransferEntity.class);
        verify(repository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TransferStatus.DONE);
    }

    @Test
    void failsWhenInsufficientFunds() {
        when(accountsClient.getAccountDetails("alice")).thenReturn(details("alice", BigDecimal.valueOf(50)));
        when(accountsClient.getAccountDetails("bob")).thenReturn(details("bob", BigDecimal.valueOf(10)));

        var result = service.process(new TransferRequest("alice", "bob", BigDecimal.valueOf(100)));

        assertThat(result).anyMatch(err -> err.contains("Недостаточно средств"));
        verify(accountsClient, never()).adjustBalance(any(), any());
        verify(repository, never()).save(any(TransferEntity.class));
    }

    @Test
    void rollsBackWhenDepositFails() {
        when(accountsClient.getAccountDetails("alice")).thenReturn(details("alice", BigDecimal.valueOf(500)));
        when(accountsClient.getAccountDetails("bob")).thenReturn(details("bob", BigDecimal.valueOf(100)));
        doReturn(details("alice", BigDecimal.valueOf(350)))
                .when(accountsClient).adjustBalance("alice",
                        new BalanceAdjustmentCommand(new BigDecimal("150.00"), OperationType.WITHDRAW));
        doThrow(new AccountsClient.AccountsClientException("boom", new RuntimeException()))
                .when(accountsClient).adjustBalance("bob",
                        new BalanceAdjustmentCommand(new BigDecimal("150.00"), OperationType.DEPOSIT));

        var result = service.process(new TransferRequest("alice", "bob", BigDecimal.valueOf(150)));

        assertThat(result).contains("boom");
        verify(accountsClient).adjustBalance("alice",
                new BalanceAdjustmentCommand(new BigDecimal("150.00"), OperationType.DEPOSIT));
        verify(repository, atLeastOnce()).save(any(TransferEntity.class));
    }

    private AccountDetails details(String login, BigDecimal balance) {
        return new AccountDetails(
                UUID.randomUUID(),
                UUID.randomUUID(),
                login,
                "ACC-" + login,
                "RUB",
                balance
        );
    }
}
