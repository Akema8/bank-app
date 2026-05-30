package ru.yandex.practicum.cash.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cash.dto.AccountDto;
import ru.yandex.practicum.cash.dto.CashRequestDto;
import ru.yandex.practicum.cash.service.CashService;

@RestController
@RequestMapping("/cash")
@RequiredArgsConstructor
public class CashController {

    private final CashService cashService;

    @PostMapping("/deposit")
    public Mono<AccountDto> deposit(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody CashRequestDto dto) {
        return cashService.deposit(jwt.getSubject(), dto);
    }

    @PostMapping("/withdraw")
    public Mono<AccountDto> withdraw(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody CashRequestDto dto) {
        return cashService.withdraw(jwt.getSubject(), dto);
    }
}
