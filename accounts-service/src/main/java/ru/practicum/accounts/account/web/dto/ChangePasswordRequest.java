package ru.practicum.accounts.account.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank
        @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
        String password
) {
}

