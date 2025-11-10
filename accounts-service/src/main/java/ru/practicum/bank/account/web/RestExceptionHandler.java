package ru.practicum.bank.account.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.bank.exception.AccountAlreadyExistsException;
import ru.practicum.bank.exception.AccountDeletionException;
import ru.practicum.bank.exception.AccountNotFoundException;

import java.util.List;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<String> handleNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ResponseEntity<List<String>> handleAlreadyExists(AccountAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(List.of(ex.getMessage()));
    }

    @ExceptionHandler(AccountDeletionException.class)
    public ResponseEntity<List<String>> handleDeletion(AccountDeletionException ex) {
        return ResponseEntity.badRequest().body(List.of(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<String>> handleValidation(MethodArgumentNotValidException ex) {
        var messages = ex.getBindingResult().getAllErrors().stream()
                .map(err -> err.getDefaultMessage() == null ? "Validation error" : err.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest().body(messages);
    }
}

