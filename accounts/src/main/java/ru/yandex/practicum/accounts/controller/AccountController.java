package ru.yandex.practicum.accounts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.accounts.dto.AccountDto;
import ru.yandex.practicum.accounts.dto.AccountRegisterDto;
import ru.yandex.practicum.accounts.dto.AccountShortDto;
import ru.yandex.practicum.accounts.dto.AccountUpdateDto;
import ru.yandex.practicum.accounts.dto.BalanceChangeDto;
import ru.yandex.practicum.accounts.service.AccountService;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AccountDto> register(@Valid @RequestBody AccountRegisterDto dto) {
        return accountService.register(dto);
    }

    @GetMapping("/me")
    public Mono<AccountDto> getMe(@AuthenticationPrincipal Jwt jwt) {
        return accountService.getByLogin(jwt.getSubject());
    }

    @PutMapping("/me")
    public Mono<AccountDto> updateMe(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody AccountUpdateDto dto) {
        return accountService.update(jwt.getSubject(), dto);
    }

    @GetMapping
    public Flux<AccountShortDto> getAll() {
        return accountService.getAll();
    }

    @PostMapping("/{login}/deposit")
    public Mono<AccountDto> deposit(@PathVariable String login,
                                    @Valid @RequestBody BalanceChangeDto dto) {
        return accountService.deposit(login, dto.amount());
    }

    @PostMapping("/{login}/withdraw")
    public Mono<AccountDto> withdraw(@PathVariable String login,
                                     @Valid @RequestBody BalanceChangeDto dto) {
        return accountService.withdraw(login, dto.amount());
    }
}
