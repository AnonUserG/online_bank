package ru.practicum.bank.service;

import java.time.LocalDate;

public class Dto {
    public record UserShort(String login, String name) {}
    public record UserProfile(String login, String name, LocalDate birthdate) {}
}
