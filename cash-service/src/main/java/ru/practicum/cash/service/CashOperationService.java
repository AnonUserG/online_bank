package ru.practicum.cash.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.cash.clients.AccountsClient;
import ru.practicum.cash.clients.NotificationsClient;
import ru.practicum.cash.clients.dto.AccountDetails;
import ru.practicum.cash.clients.dto.BalanceAdjustmentCommand;
import ru.practicum.cash.model.CashOperationEntity;
import ru.practicum.cash.model.OperationStatus;
import ru.practicum.cash.model.OperationType;
import ru.practicum.cash.repository.CashOperationRepository;
import ru.practicum.cash.web.dto.CashOperationRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class CashOperationService {

    private static final Logger log = LoggerFactory.getLogger(CashOperationService.class);

    private final CashOperationRepository repository;
    private final AccountsClient accountsClient;
    private final NotificationsClient notificationsClient;

    public CashOperationService(CashOperationRepository repository,
                                AccountsClient accountsClient,
                                NotificationsClient notificationsClient) {
        this.repository = repository;
        this.accountsClient = accountsClient;
        this.notificationsClient = notificationsClient;
    }

    @Transactional
    public List<String> process(CashOperationRequest request) {
        CashOperationEntity entity = null;
        try {
            AccountDetails account = accountsClient.getAccountDetails(request.login());
            if (account == null) {
                return List.of("Сервис аккаунтов вернул пустой ответ");
            }
            if (account.bankAccountId() == null) {
                return List.of("Для пользователя не найден банковский счёт");
            }

            OperationType type = request.action().toOperationType();
            BigDecimal amount = normalizeAmount(request.value());

            BigDecimal currentBalance = account.balance() == null ? BigDecimal.ZERO : account.balance();
            if (type == OperationType.WITHDRAW && currentBalance.compareTo(amount) < 0) {
                return List.of("Недостаточно средств на счёте");
            }

            entity = new CashOperationEntity();
            entity.setAccountId(account.bankAccountId());
            entity.setOperationType(type);
            entity.setAmount(amount);
            String currency = account.currency() == null ? "RUB" : account.currency();
            entity.setCurrency(currency);
            entity.setIdempotencyKey(UUID.randomUUID().toString());
            entity.setStatus(OperationStatus.PENDING);
            repository.save(entity);

            AccountDetails updatedAccount = accountsClient.adjustBalance(
                    request.login(),
                    new BalanceAdjustmentCommand(amount, type)
            );

            entity.setStatus(OperationStatus.DONE);
            repository.save(entity);

            String notificationCurrency = updatedAccount.currency() == null ? currency : updatedAccount.currency();
            notificationsClient.sendCashEvent(request.login(), type, amount, notificationCurrency);
            return List.of();
        } catch (AccountsClient.AccountsClientException ex) {
            log.warn("Accounts client error: {}", ex.getMessage());
            markFailed(entity);
            return List.of(ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Unexpected error while processing cash operation", ex);
            markFailed(entity);
            throw ex;
        }
    }

    private void markFailed(CashOperationEntity entity) {
        if (entity != null) {
            entity.setStatus(OperationStatus.FAILED);
            repository.save(entity);
        }
    }

    private BigDecimal normalizeAmount(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
