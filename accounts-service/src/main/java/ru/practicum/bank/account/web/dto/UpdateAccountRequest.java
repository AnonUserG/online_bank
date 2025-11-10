package ru.practicum.bank.account.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.practicum.bank.validation.Adult;

import java.time.LocalDate;

public record UpdateAccountRequest(
        @NotBlank(message = "Имя обязательно") String name,
        @NotNull(message = "Дата рождения обязательна")
        @Adult(value = 18, message = "Возраст должен быть не меньше 18 лет") LocalDate birthdate
) {
}

