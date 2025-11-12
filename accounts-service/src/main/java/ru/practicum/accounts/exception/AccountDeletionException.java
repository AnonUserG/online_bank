package ru.practicum.accounts.exception;

public class AccountDeletionException extends RuntimeException {
    public AccountDeletionException(String message) {
        super(message);
    }
}
