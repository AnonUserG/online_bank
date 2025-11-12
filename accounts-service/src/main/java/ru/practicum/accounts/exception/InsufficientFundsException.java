package ru.practicum.accounts.exception;

/**
 * Thrown when balance is not enough.
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

