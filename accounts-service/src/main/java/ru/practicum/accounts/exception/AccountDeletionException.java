package ru.practicum.accounts.exception;

/**
 * Thrown when account still contains funds.
 */
public class AccountDeletionException extends RuntimeException {
    public AccountDeletionException(String message) {
        super(message);
    }
}

