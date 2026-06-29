package ru.yandex.practicum.cash.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.cash.dto.AccountDto;
import ru.yandex.practicum.cash.dto.CashRequestDto;
import ru.yandex.practicum.common.kafka.NotificationEventProducer;
import ru.yandex.practicum.cash.model.CashTransaction;
import ru.yandex.practicum.cash.repository.CashTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({"unchecked", "rawtypes"})
class CashServiceTest {

    @Mock
    WebClient accountsWebClient;
    @Mock
    NotificationEventProducer notificationEventProducer;
    @Mock
    CashTransactionRepository repository;

    CashService cashService;

    private AccountDto accountDto;
    private CashTransaction savedTx;

    @BeforeEach
    void setUp() {
        cashService = new CashService(accountsWebClient, notificationEventProducer, repository);
        accountDto = new AccountDto(1L, "user1", "Иван", LocalDate.of(1990, 1, 1), new BigDecimal("200.00"));
        savedTx = new CashTransaction(1L, "user1", "DEPOSIT", new BigDecimal("50.00"), LocalDateTime.now());
        when(notificationEventProducer.send(anyString(), anyString())).thenReturn(Mono.empty());
    }

    private void setupAccountsChain(Mono<AccountDto> response) {
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
        when(accountsWebClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), anyString(), anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(AccountDto.class)).thenReturn(response);
    }

    @Test
    void deposit_success_savesTransactionAndReturnsAccount() {
        setupAccountsChain(Mono.just(accountDto));
        when(repository.save(any())).thenReturn(Mono.just(savedTx));

        StepVerifier.create(cashService.deposit("user1", new CashRequestDto(new BigDecimal("50.00"))))
                .assertNext(dto -> assertThat(dto.balance()).isEqualByComparingTo("200.00"))
                .verifyComplete();

        verify(repository).save(any(CashTransaction.class));
        verify(notificationEventProducer).send("user1", "Зачисление 50.00");
    }

    @Test
    void withdraw_success_savesTransactionAndReturnsAccount() {
        AccountDto withdrawDto = new AccountDto(1L, "user1", "Иван", LocalDate.of(1990, 1, 1), new BigDecimal("150.00"));
        CashTransaction withdrawTx = new CashTransaction(2L, "user1", "WITHDRAW", new BigDecimal("50.00"), LocalDateTime.now());
        setupAccountsChain(Mono.just(withdrawDto));
        when(repository.save(any())).thenReturn(Mono.just(withdrawTx));

        StepVerifier.create(cashService.withdraw("user1", new CashRequestDto(new BigDecimal("50.00"))))
                .assertNext(dto -> assertThat(dto.balance()).isEqualByComparingTo("150.00"))
                .verifyComplete();

        verify(repository).save(any(CashTransaction.class));
        verify(notificationEventProducer).send("user1", "Снятие 50.00");
    }

    @Test
    void deposit_accountsFailure_propagatesError() {
        setupAccountsChain(Mono.error(new RuntimeException("accounts unavailable")));

        StepVerifier.create(cashService.deposit("user1", new CashRequestDto(new BigDecimal("50.00"))))
                .expectError(RuntimeException.class)
                .verify();

        verify(repository, never()).save(any());
    }
}
