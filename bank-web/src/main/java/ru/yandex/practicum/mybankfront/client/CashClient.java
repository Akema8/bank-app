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
public class CashClient {

    private final RestClient accountsRestClient;

    public AccountDto deposit(BigDecimal amount) {
        return accountsRestClient.post()
                .uri("/cash/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount))
                .retrieve()
                .body(AccountDto.class);
    }

    public AccountDto withdraw(BigDecimal amount) {
        return accountsRestClient.post()
                .uri("/cash/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", amount))
                .retrieve()
                .body(AccountDto.class);
    }
}
