package ru.practicum.front.util;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

public final class ValidationUtils {
    private ValidationUtils() {}

    public static List<String> validateSignup(String login, String password, String confirm, String name, LocalDate birthdate) {
        var errors = new ArrayList<String>();
        if (isBlank(login) || isBlank(password) || isBlank(confirm) || isBlank(name) || birthdate == null) {
            errors.add("Все поля обязательны для заполнения");
        }
        if (!isBlank(password) && !password.equals(confirm)) {
            errors.add("Пароли не совпадают");
        }
        if (birthdate != null && !isAdult18(birthdate)) {
            errors.add("Возраст должен быть 18+");
        }
        return errors;
    }

    public static List<String> validatePasswordChange(String password, String confirm) {
        var errors = new ArrayList<String>();
        if (isBlank(password)) errors.add("Пароль не может быть пустым");
        if (!isBlank(password) && !password.equals(confirm)) errors.add("Пароли не совпадают");
        return errors;
    }

    public static List<String> validateProfile(String name, LocalDate birthdate) {
        var errors = new ArrayList<String>();
        if (isBlank(name) || birthdate == null) errors.add("Имя и дата рождения обязательны");
        if (birthdate != null && !isAdult18(birthdate)) errors.add("Возраст должен быть 18+");
        return errors;
    }

    private static boolean isAdult18(LocalDate birthdate) {
        return Period.between(birthdate, LocalDate.now()).getYears() >= 18;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
