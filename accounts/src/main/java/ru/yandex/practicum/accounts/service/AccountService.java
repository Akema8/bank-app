package ru.yandex.practicum.accounts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.accounts.kafka.NotificationEventProducer;
import ru.yandex.practicum.accounts.dto.AccountDto;
import ru.yandex.practicum.accounts.dto.AccountRegisterDto;
import ru.yandex.practicum.accounts.dto.AccountShortDto;
import ru.yandex.practicum.accounts.dto.AccountUpdateDto;
import ru.yandex.practicum.accounts.model.Account;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final NotificationEventProducer notificationEventProducer;

    public Mono<AccountDto> register(AccountRegisterDto dto) {
        return validateAge(dto.birthdate())
                .then(Mono.defer(() -> accountRepository.existsByLogin(dto.login())))
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.CONFLICT, "Такой логин уже существует: " + dto.login()));
                    }
                    Account account = new Account(null, dto.login(), dto.name(), dto.birthdate(), BigDecimal.ZERO);
                    return accountRepository.save(account);
                })
                .map(this::toDto);
    }

    public Mono<AccountDto> getByLogin(String login) {
        return findOrThrow(login).map(this::toDto);
    }

    public Mono<AccountDto> update(String login, AccountUpdateDto dto) {
        return validateAge(dto.birthdate())
                .then(Mono.defer(() -> findOrThrow(login)))
                .flatMap(account -> {
                    account.setName(dto.name());
                    account.setBirthdate(dto.birthdate());
                    return accountRepository.save(account);
                })
                .map(this::toDto);
    }

    public Mono<AccountDto> deposit(String login, BigDecimal amount) {
        return findOrThrow(login)
                .flatMap(account -> {
                    account.setBalance(account.getBalance().add(amount));
                    return accountRepository.save(account);
                })
                .map(this::toDto)
                .flatMap(dto -> notificationEventProducer.send(login, "Пополнение счёта: +" + amount)
                        .thenReturn(dto));
    }

    public Mono<AccountDto> withdraw(String login, BigDecimal amount) {
        return findOrThrow(login)
                .flatMap(account -> {
                    if (account.getBalance().compareTo(amount) < 0) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Недостаточно средств на счёте"));
                    }
                    account.setBalance(account.getBalance().subtract(amount));
                    return accountRepository.save(account);
                })
                .map(this::toDto)
                .flatMap(dto -> notificationEventProducer.send(login, "Снятие со счёта: -" + amount)
                        .thenReturn(dto));
    }

    public Flux<AccountShortDto> getAll() {
        return accountRepository.findAll()
                .map(a -> new AccountShortDto(a.getLogin(), a.getName()));
    }

    private Mono<Account> findOrThrow(String login) {
        return accountRepository.findByLogin(login)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Такой акаунт не найден: " + login)));
    }

    private Mono<Void> validateAge(LocalDate birthdate) {
        if (birthdate.isAfter(LocalDate.now().minusYears(18))) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Вам должно быть больше 18 лет"));
        }
        return Mono.empty();
    }

    private AccountDto toDto(Account a) {
        return new AccountDto(a.getId(), a.getLogin(), a.getName(), a.getBirthdate(), a.getBalance());
    }
}