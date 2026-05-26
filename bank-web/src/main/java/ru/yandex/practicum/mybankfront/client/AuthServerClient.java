package ru.yandex.practicum.mybankfront.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthServerClient {

    private final RestClient authRestClient;

    public void register(String login, String password) {
        authRestClient.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", login, "password", password))
                .retrieve()
                .toBodilessEntity();
    }
}
