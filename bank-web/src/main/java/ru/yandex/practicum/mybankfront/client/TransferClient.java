package ru.yandex.practicum.mybankfront.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TransferClient {

    private final RestClient accountsRestClient;

    public AccountDto transfer(String toLogin, BigDecimal amount) {
        return accountsRestClient.post()
                .uri("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("toLogin", toLogin, "amount", amount))
                .retrieve()
                .body(AccountDto.class);
    }
}
