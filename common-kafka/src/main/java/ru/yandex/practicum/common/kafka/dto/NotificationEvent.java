package ru.yandex.practicum.common.kafka.dto;

public record NotificationEvent(String login, String message) {
}
