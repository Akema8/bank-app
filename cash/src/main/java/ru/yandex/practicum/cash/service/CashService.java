package ru.yandex.practicum.cash.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cash.dto.AccountDto;
import ru.yandex.practicum.cash.dto.CashRequestDto;
import ru.yandex.practicum.common.kafka.NotificationEventProducer;
import ru.yandex.practicum.cash.model.CashTransaction;
import ru.yandex.practicum.cash.repository.CashTransactionRepository;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashService {

    private final WebClient accountsWebClient;
    private final NotificationEventProducer notificationEventProducer;
    private final CashTransactionRepository repository;

    public Mono<AccountDto> deposit(String login, CashRequestDto dto) {
        return changeBalance(login, "deposit", dto)
                .flatMap(account -> save(login, "DEPOSIT", dto)
                        .then(notificationEventProducer.send(login, "Зачисление " + dto.amount()))
                        .thenReturn(account));
    }

    public Mono<AccountDto> withdraw(String login, CashRequestDto dto) {
        return changeBalance(login, "withdraw", dto)
                .flatMap(account -> save(login, "WITHDRAW", dto)
                        .then(notificationEventProducer.send(login, "Снятие " + dto.amount()))
                        .thenReturn(account));
    }

    private Mono<AccountDto> changeBalance(String login, String action, CashRequestDto dto) {
        return accountsWebClient.post()
                .uri("/accounts/{login}/{action}", login, action)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("amount", dto.amount()))
                .retrieve()
                .bodyToMono(AccountDto.class);
    }

    private Mono<CashTransaction> save(String login, String action, CashRequestDto dto) {
        return repository.save(new CashTransaction(null, login, action, dto.amount(), LocalDateTime.now()));
    }
}
