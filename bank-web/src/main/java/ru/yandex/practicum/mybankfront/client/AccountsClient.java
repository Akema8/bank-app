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

    private final RestClient restClient;

    public AccountDto register(AccountRegisterDto dto) {
        return restClient.post()
                .uri("/accounts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .body(AccountDto.class);
    }

    public AccountDto getByLogin(String login) {
        return restClient.get()
                .uri("/accounts/me")
                .header("X-Login", login)
                .retrieve()
                .body(AccountDto.class);
    }

    public AccountDto update(String login, String name, LocalDate birthdate) {
        return restClient.put()
                .uri("/accounts/me")
                .header("X-Login", login)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", name, "birthdate", birthdate.toString()))
                .retrieve()
                .body(AccountDto.class);
    }

    public List<AccountShortDto> getAll() {
        return restClient.get()
                .uri("/accounts")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
