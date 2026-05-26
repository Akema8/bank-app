package ru.yandex.practicum.mybankfront.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthServerClient {

    private final RestClient authRestClient;

    public void register(String login, String password) {
        ResponseEntity<Void> response = authRestClient.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("login", login, "password", password))
                .retrieve()
                .toBodilessEntity();
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RestClientException("Auth-server вернул: " + response.getStatusCode());
        }
    }
}
