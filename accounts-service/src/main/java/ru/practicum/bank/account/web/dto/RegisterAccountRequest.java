package ru.practicum.bank.account.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record RegisterAccountRequest(
        @NotBlank String login,
        @NotBlank String password,
        @NotBlank String name,
        @Email(message = "Некорректный email")
        String email,
        @NotNull @Past LocalDate birthdate
) {
}

