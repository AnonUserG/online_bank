package ru.practicum.accounts.exception;

/**
 * Thrown when user is not found.
 */
public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}

