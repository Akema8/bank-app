package ru.yandex.practicum.accounts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record AccountUpdateDto(
        @NotBlank String name,
        @NotNull @Past LocalDate birthdate
) {
}