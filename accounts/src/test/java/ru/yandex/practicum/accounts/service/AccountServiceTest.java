package ru.yandex.practicum.accounts.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.accounts.client.NotificationsClient;
import ru.yandex.practicum.accounts.dto.AccountDto;
import ru.yandex.practicum.accounts.dto.AccountRegisterDto;
import ru.yandex.practicum.accounts.dto.AccountUpdateDto;
import ru.yandex.practicum.accounts.model.Account;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private NotificationsClient notificationsClient;

    @InjectMocks
    private AccountService accountService;

    private static final LocalDate ADULT_BIRTHDATE = LocalDate.now().minusYears(25);
    private static final LocalDate MINOR_BIRTHDATE = LocalDate.now().minusYears(17);

    @Test
    void register_success() {
        AccountRegisterDto dto = new AccountRegisterDto("user1", "Иван Иванов", ADULT_BIRTHDATE);
        Account saved = new Account(1L, "user1", "Иван Иванов", ADULT_BIRTHDATE, BigDecimal.ZERO);

        when(accountRepository.existsByLogin("user1")).thenReturn(Mono.just(false));
        when(accountRepository.save(any())).thenReturn(Mono.just(saved));

        StepVerifier.create(accountService.register(dto))
                .assertNext(result -> {
                    assertThat(result.login()).isEqualTo("user1");
                    assertThat(result.balance()).isEqualByComparingTo(BigDecimal.ZERO);
                })
                .verifyComplete();
    }

    @Test
    void register_loginAlreadyExists_returnsConflict() {
        AccountRegisterDto dto = new AccountRegisterDto("user1", "Иван Иванов", ADULT_BIRTHDATE);
        when(accountRepository.existsByLogin("user1")).thenReturn(Mono.just(true));

        StepVerifier.create(accountService.register(dto))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.CONFLICT)
                .verify();
    }

    @Test
    void register_underage_returnsBadRequest() {
        AccountRegisterDto dto = new AccountRegisterDto("user1", "Иван Иванов", MINOR_BIRTHDATE);

        StepVerifier.create(accountService.register(dto))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void getByLogin_success() {
        Account account = new Account(1L, "user1", "Иван Иванов", ADULT_BIRTHDATE, BigDecimal.TEN);
        when(accountRepository.findByLogin("user1")).thenReturn(Mono.just(account));

        StepVerifier.create(accountService.getByLogin("user1"))
                .assertNext(result -> assertThat(result.login()).isEqualTo("user1"))
                .verifyComplete();
    }

    @Test
    void getByLogin_notFound_returnsNotFound() {
        when(accountRepository.findByLogin("unknown")).thenReturn(Mono.empty());

        StepVerifier.create(accountService.getByLogin("unknown"))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void update_success() {
        Account existing = new Account(1L, "user1", "Старое Имя", ADULT_BIRTHDATE, BigDecimal.TEN);
        AccountUpdateDto dto = new AccountUpdateDto("Новое Имя", ADULT_BIRTHDATE.plusYears(1));
        Account updated = new Account(1L, "user1", "Новое Имя", dto.birthdate(), BigDecimal.TEN);

        when(accountRepository.findByLogin("user1")).thenReturn(Mono.just(existing));
        when(accountRepository.save(any())).thenReturn(Mono.just(updated));

        StepVerifier.create(accountService.update("user1", dto))
                .assertNext(result -> assertThat(result.name()).isEqualTo("Новое Имя"))
                .verifyComplete();
    }

    @Test
    void update_underage_returnsBadRequest() {
        AccountUpdateDto dto = new AccountUpdateDto("Имя", MINOR_BIRTHDATE);

        StepVerifier.create(accountService.update("user1", dto))
                .expectErrorMatches(ex -> ex instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void getAll_returnsShortDtos() {
        Account a1 = new Account(1L, "user1", "Иван Иванов", ADULT_BIRTHDATE, BigDecimal.ZERO);
        Account a2 = new Account(2L, "user2", "Пётр Петров", ADULT_BIRTHDATE, BigDecimal.TEN);
        when(accountRepository.findAll()).thenReturn(Flux.just(a1, a2));

        StepVerifier.create(accountService.getAll())
                .assertNext(dto -> assertThat(dto.login()).isEqualTo("user1"))
                .assertNext(dto -> assertThat(dto.login()).isEqualTo("user2"))
                .verifyComplete();
    }

    @Test
    void getAll_empty_returnsEmpty() {
        when(accountRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(accountService.getAll())
                .verifyComplete();
    }

    @Test
    void getByLogin_mapsAllFields() {
        LocalDate birthdate = LocalDate.of(1990, 6, 15);
        Account account = new Account(42L, "user1", "Иван Иванов", birthdate, new BigDecimal("1500.50"));
        when(accountRepository.findByLogin("user1")).thenReturn(Mono.just(account));

        StepVerifier.create(accountService.getByLogin("user1"))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(42L);
                    assertThat(dto.login()).isEqualTo("user1");
                    assertThat(dto.name()).isEqualTo("Иван Иванов");
                    assertThat(dto.birthdate()).isEqualTo(birthdate);
                    assertThat(dto.balance()).isEqualByComparingTo("1500.50");
                })
                .verifyComplete();
    }
}