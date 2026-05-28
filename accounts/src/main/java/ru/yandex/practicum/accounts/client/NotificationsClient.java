package ru.yandex.practicum.accounts.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationsClient {

    private final WebClient notificationsWebClient;

    public Mono<Void> notify(String login, String message) {
        return notificationsWebClient.post()
                .uri("/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("login", login, "message", message))
                .retrieve()
                .toBodilessEntity()
                .doOnError(e -> log.warn("Notifications call failed for {}: {}", login, e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }
}