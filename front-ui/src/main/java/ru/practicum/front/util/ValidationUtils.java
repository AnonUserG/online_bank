package ru.practicum.front.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static List<String> validateSignup(String login,
                                              String password,
                                              String confirm,
                                              String name,
                                              LocalDate birthdate) {
        var errors = new ArrayList<String>();
        if (isBlank(login) || isBlank(password) || isBlank(confirm) || isBlank(name) || birthdate == null) {
            errors.add("Все поля регистрации обязательны");
        }
        if (!isBlank(password) && !password.equals(confirm)) {
            errors.add("Пароли не совпадают");
        }
        if (birthdate != null && !isAdult18(birthdate)) {
            errors.add("Пользователь должен быть старше 18 лет");
        }
        return errors;
    }

    public static List<String> validatePasswordChange(String password, String confirm) {
        var errors = new ArrayList<String>();
        if (isBlank(password)) {
            errors.add("Пароль обязателен");
        } else {
            if (password.length() < 6) {
                errors.add("Пароль должен быть не короче 6 символов");
            }
            if (!password.equals(confirm)) {
                errors.add("Пароли не совпадают");
            }
        }
        return errors;
    }

    public static List<String> validateProfile(String name, LocalDate birthdate) {
        var errors = new ArrayList<String>();
        if (isBlank(name) || birthdate == null) {
            errors.add("Имя и дата рождения обязательны");
        }
        if (birthdate != null && !isAdult18(birthdate)) {
            errors.add("Пользователь должен быть старше 18 лет");
        }
        return errors;
    }

    public static List<String> validateTransfer(String toLogin, String value) {
        var errors = new ArrayList<String>();
        if (isBlank(toLogin)) {
            errors.add("Выберите получателя перевода");
        }
        if (isBlank(value)) {
            errors.add("Укажите сумму перевода");
        } else {
            try {
                BigDecimal amount = new BigDecimal(value.trim());
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Сумма должна быть больше нуля");
                }
            } catch (NumberFormatException ex) {
                errors.add("Сумма должна быть числом");
            }
        }
        return errors;
    }

    private static boolean isAdult18(LocalDate birthdate) {
        return Period.between(birthdate, LocalDate.now()).getYears() >= 18;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
