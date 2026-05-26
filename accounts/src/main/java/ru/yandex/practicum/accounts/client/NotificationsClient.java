package ru.yandex.practicum.accounts.client;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

// TODO: заменить на реальный HTTP-клиент при реализации сервиса Notifications
@Component
public class NotificationsClient {

    public Mono<Void> notify(String login, String message) {
        return Mono.empty();
    }
}