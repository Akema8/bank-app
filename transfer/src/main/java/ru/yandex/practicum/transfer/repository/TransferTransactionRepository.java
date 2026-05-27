package ru.yandex.practicum.transfer.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import ru.yandex.practicum.transfer.model.TransferTransaction;

public interface TransferTransactionRepository extends ReactiveCrudRepository<TransferTransaction, Long> {
}
