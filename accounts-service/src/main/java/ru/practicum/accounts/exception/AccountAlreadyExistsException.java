package ru.practicum.accounts.exception;

/**
 * Thrown when login already exists.
 */
public class AccountAlreadyExistsException extends RuntimeException {
    public AccountAlreadyExistsException(String message) {
        super(message);
    }
}

