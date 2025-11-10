package ru.practicum.bank.exception;

public class AccountDeletionException extends RuntimeException {
    public AccountDeletionException(String message) {
        super(message);
    }
}
