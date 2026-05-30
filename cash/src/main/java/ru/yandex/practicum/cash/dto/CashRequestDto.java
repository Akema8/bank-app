package ru.yandex.practicum.cash.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CashRequestDto(
        @NotNull @Positive BigDecimal amount
) {
}
