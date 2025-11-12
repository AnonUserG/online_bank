package ru.practicum.accounts.account.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.practicum.accounts.validation.Adult;

import java.time.LocalDate;

public record RegisterAccountRequest(
        @NotBlank(message = "Логин обязателен") String login,
        @NotBlank(message = "Пароль обязателен") String password,
        @NotBlank(message = "Имя обязательно") String name,
        @Email(message = "Некорректный email")
        String email,
        @NotNull(message = "Дата рождения обязательна")
        @Adult(value = 18, message = "Возраст должен быть не меньше 18 лет")
        LocalDate birthdate
) {
}

