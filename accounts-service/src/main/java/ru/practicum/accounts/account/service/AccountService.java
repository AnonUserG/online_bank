package ru.practicum.accounts.account.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.accounts.account.model.AccountEntity;
import ru.practicum.accounts.account.model.BankAccountEntity;
import ru.practicum.accounts.account.repository.AccountRepository;
import ru.practicum.accounts.account.repository.BankAccountRepository;
import ru.practicum.accounts.account.web.dto.AccountDetailsDto;
import ru.practicum.accounts.account.web.dto.AccountDto;
import ru.practicum.accounts.account.web.dto.BalanceAdjustmentRequest;
import ru.practicum.accounts.account.web.dto.BalanceOperationType;
import ru.practicum.accounts.account.web.dto.ChangePasswordRequest;
import ru.practicum.accounts.account.web.dto.RegisterAccountRequest;
import ru.practicum.accounts.account.web.dto.UpdateAccountRequest;
import ru.practicum.accounts.clients.KeycloakAdminClient;
import ru.practicum.accounts.clients.NotificationsClient;
import ru.practicum.accounts.exception.AccountAlreadyExistsException;
import ru.practicum.accounts.exception.AccountDeletionException;
import ru.practicum.accounts.exception.AccountNotFoundException;
import ru.practicum.accounts.exception.InsufficientFundsException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

/**
 * Core business logic for working with accounts.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository repository;
    private final BankAccountRepository bankAccountRepository;
    private final AccountMapper mapper;
    private final KeycloakAdminClient keycloakAdminClient;
    private final NotificationsClient notificationsClient;
    private final SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public AccountDto getProfile(String login) {
        var entity = repository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException("User '%s' not found".formatted(login)));
        return mapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AccountDetailsDto getAccountDetails(String login) {
        var entity = repository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException("User '%s' not found".formatted(login)));
        return mapper.toDetailsDto(entity);
    }

    @Transactional
    public List<String> updateProfile(String login, UpdateAccountRequest request) {
        var entity = repository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException("User '%s' not found".formatted(login)));

        entity.setName(request.name());
        entity.setBirthdate(request.birthdate());
        repository.save(entity);

        notificationsClient.sendProfileUpdated(login);
        return List.of();
    }

    @Transactional
    public List<String> register(RegisterAccountRequest request) {
        if (repository.existsByLogin(request.login())) {
            throw new AccountAlreadyExistsException("Login already registered");
        }

        var kcId = keycloakAdminClient.createUser(request.login(), request.password(), request.name(), request.email());

        var account = new AccountEntity();
        account.setLogin(request.login().toLowerCase(Locale.ROOT));
        account.setName(request.name());
        account.setEmail(request.email());
        account.setBirthdate(request.birthdate());
        account.setKeycloakId(kcId);

        var bankAccount = new BankAccountEntity();
        bankAccount.setAccountNumber(generateAccountNumber());
        bankAccount.setCurrency("RUB");
        bankAccount.setBalance(BigDecimal.ZERO);
        account.setBankAccount(bankAccount);

        repository.save(account);
        notificationsClient.sendRegistrationCompleted(request.login());
        return List.of();
    }

    @Transactional
    public List<String> changePassword(String login, ChangePasswordRequest request) {
        repository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException("User '%s' not found".formatted(login)));

        keycloakAdminClient.resetPassword(login, request.password());
        notificationsClient.sendPasswordChanged(login);
        return List.of();
    }

    @Transactional
    public List<String> deleteAccount(String login) {
        var entity = repository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException("User '%s' not found".formatted(login)));

        var bank = entity.getBankAccount();
        var balance = bank != null ? bank.getBalance() : BigDecimal.ZERO;
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            throw new AccountDeletionException("Account contains funds and cannot be removed");
        }

        keycloakAdminClient.deleteUser(login);
        repository.delete(entity);
        notificationsClient.sendAccountDeleted(login);
        return List.of();
    }

    @Transactional
    public AccountDetailsDto adjustBalance(String login, BalanceAdjustmentRequest request) {
        var bank = bankAccountRepository.findByUserLoginForUpdate(login)
                .orElseThrow(() -> new AccountNotFoundException("User '%s' not found".formatted(login)));
        var entity = bank.getUser();

        var amount = request.amount();
        if (request.type() == BalanceOperationType.DEPOSIT) {
            bank.setBalance(bank.getBalance().add(amount));
        } else {
            if (bank.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException("Insufficient funds on source account");
            }
            bank.setBalance(bank.getBalance().subtract(amount));
        }

        bankAccountRepository.save(bank);
        entity.setBankAccount(bank);
        return mapper.toDetailsDto(entity);
    }

    private String generateAccountNumber() {
        var builder = new StringBuilder("4080");
        while (builder.length() < 20) {
            builder.append(random.nextInt(10));
        }
        return builder.toString();
    }
}



