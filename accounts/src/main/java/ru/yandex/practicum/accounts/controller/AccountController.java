package ru.yandex.practicum.accounts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.accounts.dto.AccountDto;
import ru.yandex.practicum.accounts.dto.AccountRegisterDto;
import ru.yandex.practicum.accounts.dto.AccountShortDto;
import ru.yandex.practicum.accounts.dto.AccountUpdateDto;
import ru.yandex.practicum.accounts.service.AccountService;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // TODO: заменить на @AuthenticationPrincipal Jwt при подключении OAuth2
    private static final String LOGIN_HEADER = "X-Login";

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AccountDto> register(@Valid @RequestBody AccountRegisterDto dto) {
        return accountService.register(dto);
    }

    @GetMapping("/me")
    public Mono<AccountDto> getMe(@RequestHeader(LOGIN_HEADER) String login) {
        return accountService.getByLogin(login);
    }

    @PutMapping("/me")
    public Mono<AccountDto> updateMe(@RequestHeader(LOGIN_HEADER) String login,
                                     @Valid @RequestBody AccountUpdateDto dto) {
        return accountService.update(login, dto);
    }

    @GetMapping
    public Flux<AccountShortDto> getAll() {
        return accountService.getAll();
    }
}