package ru.yandex.practicum.mybankfront.controller.dto;

import java.time.LocalDate;

public record AccountRegisterDto(
        String login,
        String name,
        LocalDate birthdate
) {
}
