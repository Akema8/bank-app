package ru.yandex.practicum.cash.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import ru.yandex.practicum.cash.model.CashTransaction;

public interface CashTransactionRepository extends ReactiveCrudRepository<CashTransaction, Long> {
}
