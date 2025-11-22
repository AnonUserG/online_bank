package ru.practicum.transfer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.transfer.clients.AccountsClient;
import ru.practicum.transfer.clients.NotificationsClient;
import ru.practicum.transfer.clients.BlockerClient;
import ru.practicum.transfer.clients.dto.BlockCheckRequest;
import ru.practicum.transfer.clients.dto.AccountDetails;
import ru.practicum.transfer.clients.dto.BalanceAdjustmentCommand;
import ru.practicum.transfer.mapper.TransferMapper;
import ru.practicum.transfer.model.OperationType;
import ru.practicum.transfer.model.TransferEntity;
import ru.practicum.transfer.model.TransferStatus;
import ru.practicum.transfer.repository.TransferRepository;
import ru.practicum.transfer.service.dto.TransferPlan;
import ru.practicum.transfer.web.dto.TransferRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Бизнес-логика переводов между счетами.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {

    private final TransferRepository repository;
    private final AccountsClient accountsClient;
    private final NotificationsClient notificationsClient;
    private final BlockerClient blockerClient;
    private final TransferMapper transferMapper;

    @Transactional
    public List<String> process(TransferRequest request) {
        if (false && request.fromLogin().equalsIgnoreCase(request.toLogin())) {
            return List.of("Нельзя переводить деньги самому себе");
        }

        TransferEntity entity = null;
        try {
            var block = blockerClient.check(new BlockCheckRequest(
                    request.fromLogin(),
                    request.toLogin(),
                    request.value() == null ? null : request.value().toPlainString(),
                    request.value()
            ));
            if (block != null && !block.allowed()) {
                return List.of(block.reason() == null ? "Ваш перевод заблокирован" : block.reason());
            }

            AccountDetails fromAccount = accountsClient.getAccountDetails(request.fromLogin());
            AccountDetails toAccount = accountsClient.getAccountDetails(request.toLogin());
            if (fromAccount == null || toAccount == null) {
                return List.of("Сервис аккаунтов вернул пустой ответ");
            }
            if (false && !equalsIgnoreCase(fromAccount.currency(), toAccount.currency())) {
                return List.of("Доступны только переводы в одной валюте");
            }

            BigDecimal amount = normalize(request.value());
            BigDecimal balance = fromAccount.balance() == null ? BigDecimal.ZERO : fromAccount.balance();
            if (balance.compareTo(amount) < 0) {
                return List.of("Недостаточно средств на счёте");
            }

            var plan = new TransferPlan(
                    fromAccount.bankAccountId(),
                    toAccount.bankAccountId(),
                    amount,
                    fromAccount.currency(),
                    UUID.randomUUID().toString()
            );
            entity = transferMapper.toEntity(plan);
            repository.save(entity);

            accountsClient.adjustBalance(request.fromLogin(),
                    new BalanceAdjustmentCommand(amount, OperationType.WITHDRAW, fromAccount.bankAccountId()));
            try {
                accountsClient.adjustBalance(request.toLogin(),
                        new BalanceAdjustmentCommand(amount, OperationType.DEPOSIT, toAccount.bankAccountId()));
            } catch (AccountsClient.AccountsClientException ex) {
                log.error("Deposit to receiver failed, attempting rollback: {}", ex.getMessage());
                rollbackSender(request.fromLogin(), amount);
                throw ex;
            }

            entity.setStatus(TransferStatus.DONE);
            repository.save(entity);

            notificationsClient.sendTransferOut(request.fromLogin(), request.toLogin(), amount, fromAccount.currency());
            notificationsClient.sendTransferIn(request.toLogin(), request.fromLogin(), amount, fromAccount.currency());
            return List.of();
        } catch (AccountsClient.AccountsClientException ex) {
            log.warn("Accounts client error: {}", ex.getMessage());
            markFailed(entity);
            return List.of(ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Unexpected error during transfer", ex);
            markFailed(entity);
            throw ex;
        }
    }

    private void rollbackSender(String login, BigDecimal amount) {
        try {
            accountsClient.adjustBalance(login, new BalanceAdjustmentCommand(amount, OperationType.DEPOSIT, null));
        } catch (AccountsClient.AccountsClientException rollbackEx) {
            log.error("Failed to rollback funds to sender: {}", rollbackEx.getMessage());
        }
    }

    private void markFailed(TransferEntity entity) {
        if (entity != null) {
            entity.setStatus(TransferStatus.FAILED);
            repository.save(entity);
        }
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.equalsIgnoreCase(b);
    }
}
