package ru.practicum.accounts.account.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import ru.practicum.accounts.validation.Adult;

/**
 * Payload for user registration.
 */
public record RegisterAccountRequest(
        @NotBlank(message = "login is required")
        String login,
        @NotBlank(message = "password is required")
        String password,
        @NotBlank(message = "name is required")
        String name,
        @Email(message = "email must be valid")
        String email,
        @NotNull(message = "birthdate is required")
        @Adult(value = 18, message = "user must be at least 18 years old")
        LocalDate birthdate
) {
}

