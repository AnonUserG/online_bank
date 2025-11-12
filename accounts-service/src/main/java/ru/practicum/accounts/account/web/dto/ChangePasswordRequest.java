package ru.practicum.accounts.account.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for password change.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "password is required")
        @Size(min = 6, message = "password must contain at least 6 characters")
        String password
) {
}

