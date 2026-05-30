package ru.yandex.practicum.cash.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountDto(
        Long id,
        String login,
        String name,
        LocalDate birthdate,
        BigDecimal balance
) {
}
