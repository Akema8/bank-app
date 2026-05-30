package ru.yandex.practicum.accounts.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.accounts.model.Account;

public interface AccountRepository extends ReactiveCrudRepository<Account, Long> {

    Mono<Account> findByLogin(String login);

    Mono<Boolean> existsByLogin(String login);
}