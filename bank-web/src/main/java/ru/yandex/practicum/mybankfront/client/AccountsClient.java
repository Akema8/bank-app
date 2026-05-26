package ru.yandex.practicum.mybankfront.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountRegisterDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountShortDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AccountsClient {

    private final RestClient accountsRestClient;

    public AccountDto register(AccountRegisterDto dto) {
        return accountsRestClient.post()
                .uri("/accounts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(AccountDto.class);
    }

    public AccountDto getMe() {
        return accountsRestClient.get()
                .uri("/accounts/me")
                .retrieve()
                .body(AccountDto.class);
    }

    public AccountDto update(String name, LocalDate birthdate) {
        return accountsRestClient.put()
                .uri("/accounts/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", name, "birthdate", birthdate.toString()))
                .retrieve()
                .body(AccountDto.class);
    }

    public List<AccountShortDto> getAll() {
        return accountsRestClient.get()
                .uri("/accounts")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
