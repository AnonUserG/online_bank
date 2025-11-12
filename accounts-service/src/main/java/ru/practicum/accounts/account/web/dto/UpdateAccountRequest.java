package ru.practicum.accounts.account.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import ru.practicum.accounts.validation.Adult;

/**
 * Payload for profile updates.
 */
public record UpdateAccountRequest(
        @NotBlank(message = "name is required")
        String name,
        @NotNull(message = "birthdate is required")
        @Adult(value = 18, message = "user must be at least 18 years old")
        LocalDate birthdate
) {
}

