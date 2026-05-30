package ru.yandex.practicum.transfer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.transfer.dto.AccountDto;
import ru.yandex.practicum.transfer.dto.TransferRequestDto;
import ru.yandex.practicum.transfer.model.TransferTransaction;
import ru.yandex.practicum.transfer.repository.TransferTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final WebClient accountsWebClient;
    private final WebClient notificationsWebClient;
    private final TransferTransactionRepository repository;

    public Mono<AccountDto> transfer(String fromLogin, TransferRequestDto dto) {
        if (fromLogin.equals(dto.toLogin())) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Нельзя переводить деньги самому себе"));
        }

        return withdraw(fromLogin, dto.amount())
                .flatMap(senderAccount ->
                        deposit(dto.toLogin(), dto.amount())
                                .onErrorResume(depositError ->
                                        // Компенсация: вернуть деньги отправителю
                                        deposit(fromLogin, dto.amount())
                                                .then(saveFailed(fromLogin, dto, depositError))
                                                .then(Mono.error(new ResponseStatusException(
                                                        HttpStatus.BAD_REQUEST,
                                                        "Перевод не выполнен: " + depositError.getMessage())))
                                )
                                .flatMap(toAccount ->
                                        saveSuccess(fromLogin, dto)
                                                .then(notify(fromLogin, "Перевод " + dto.amount() + " пользователю " + dto.toLogin()))
                                                .then(notify(dto.toLogin(), "Получен перевод " + dto.amount() + " от " + fromLogin))
                                                .thenReturn(senderAccount)
                                )
                );
    }

    private Mono<AccountDto> withdraw(String login, BigDecimal amount) {
        return accountsWebClient.post()
                .uri("/accounts/{login}/withdraw", login)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("amount", amount))
                .retrieve()
                .bodyToMono(AccountDto.class);
    }

    private Mono<AccountDto> deposit(String login, BigDecimal amount) {
        return accountsWebClient.post()
                .uri("/accounts/{login}/deposit", login)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("amount", amount))
                .retrieve()
                .bodyToMono(AccountDto.class);
    }

    private Mono<TransferTransaction> saveSuccess(String fromLogin, TransferRequestDto dto) {
        return repository.save(new TransferTransaction(
                null, fromLogin, dto.toLogin(), dto.amount(), "SUCCESS", null, LocalDateTime.now()));
    }

    private Mono<TransferTransaction> saveFailed(String fromLogin, TransferRequestDto dto, Throwable err) {
        String msg = err.getMessage();
        if (msg != null && msg.length() > 500) msg = msg.substring(0, 500);
        return repository.save(new TransferTransaction(
                null, fromLogin, dto.toLogin(), dto.amount(), "FAILED", msg, LocalDateTime.now()));
    }

    private Mono<Void> notify(String login, String message) {
        return notificationsWebClient.post()
                .uri("/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("login", login, "message", message))
                .retrieve()
                .toBodilessEntity()
                .doOnError(e -> log.warn("Notifications call failed for {}: {}", login, e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }
}
