package ru.yandex.practicum.transfer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.transfer.dto.AccountDto;
import ru.yandex.practicum.transfer.dto.TransferRequestDto;
import ru.yandex.practicum.common.kafka.NotificationEventProducer;
import ru.yandex.practicum.transfer.model.TransferTransaction;
import ru.yandex.practicum.transfer.repository.TransferTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({"unchecked", "rawtypes"})
class TransferServiceTest {

    @Mock
    WebClient accountsWebClient;
    @Mock
    NotificationEventProducer notificationEventProducer;
    @Mock
    TransferTransactionRepository repository;

    TransferService transferService;

    private WebClient.RequestBodyUriSpec accountsUriSpec;
    private WebClient.RequestBodySpec withdrawBodySpec;
    private WebClient.RequestBodySpec depositBodySpec;
    private WebClient.RequestHeadersSpec withdrawHeadersSpec;
    private WebClient.RequestHeadersSpec depositHeadersSpec;
    private WebClient.ResponseSpec withdrawResponseSpec;
    private WebClient.ResponseSpec depositResponseSpec;

    private AccountDto senderAccount;
    private AccountDto recipientAccount;
    private TransferTransaction successTx;
    private TransferTransaction failedTx;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(accountsWebClient, notificationEventProducer, repository);

        senderAccount = new AccountDto(1L, "sender", "Иван", LocalDate.of(1990, 1, 1), new BigDecimal("400.00"));
        recipientAccount = new AccountDto(2L, "recipient", "Мария", LocalDate.of(1992, 5, 15), new BigDecimal("600.00"));
        successTx = new TransferTransaction(1L, "sender", "recipient", new BigDecimal("100.00"), "SUCCESS", null, LocalDateTime.now());
        failedTx = new TransferTransaction(2L, "sender", "recipient", new BigDecimal("100.00"), "FAILED", "deposit failed", LocalDateTime.now());

        accountsUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        withdrawBodySpec = mock(WebClient.RequestBodySpec.class);
        depositBodySpec = mock(WebClient.RequestBodySpec.class);
        withdrawHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        depositHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        withdrawResponseSpec = mock(WebClient.ResponseSpec.class);
        depositResponseSpec = mock(WebClient.ResponseSpec.class);

        when(accountsWebClient.post()).thenReturn(accountsUriSpec);
        when(accountsUriSpec.uri(contains("withdraw"), anyString())).thenReturn(withdrawBodySpec);
        when(accountsUriSpec.uri(contains("deposit"), anyString())).thenReturn(depositBodySpec);
        when(withdrawBodySpec.contentType(any())).thenReturn(withdrawBodySpec);
        when(withdrawBodySpec.bodyValue(any())).thenReturn(withdrawHeadersSpec);
        when(withdrawHeadersSpec.retrieve()).thenReturn(withdrawResponseSpec);
        when(depositBodySpec.contentType(any())).thenReturn(depositBodySpec);
        when(depositBodySpec.bodyValue(any())).thenReturn(depositHeadersSpec);
        when(depositHeadersSpec.retrieve()).thenReturn(depositResponseSpec);

        when(notificationEventProducer.send(anyString(), anyString())).thenReturn(Mono.empty());
    }

    @Test
    void transfer_selfTransfer_returnsBadRequest() {
        StepVerifier.create(transferService.transfer("user1",
                        new TransferRequestDto("user1", new BigDecimal("100"))))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void transfer_success_returnsUpdatedSenderAccount() {
        when(withdrawResponseSpec.bodyToMono(AccountDto.class)).thenReturn(Mono.just(senderAccount));
        when(depositResponseSpec.bodyToMono(AccountDto.class)).thenReturn(Mono.just(recipientAccount));
        when(repository.save(any())).thenReturn(Mono.just(successTx));

        StepVerifier.create(transferService.transfer("sender",
                        new TransferRequestDto("recipient", new BigDecimal("100.00"))))
                .assertNext(dto -> assertThat(dto.login()).isEqualTo("sender"))
                .verifyComplete();

        verify(repository).save(any(TransferTransaction.class));
        verify(notificationEventProducer, times(2)).send(anyString(), anyString());
    }

    @Test
    void transfer_depositFails_compensatesAndReturnsBadRequest() {
        RuntimeException depositError = new RuntimeException("insufficient funds at recipient");
        when(withdrawResponseSpec.bodyToMono(AccountDto.class)).thenReturn(Mono.just(senderAccount));
        when(depositResponseSpec.bodyToMono(AccountDto.class))
                .thenReturn(Mono.error(depositError))
                .thenReturn(Mono.just(senderAccount));
        when(repository.save(any())).thenReturn(Mono.just(failedTx));

        StepVerifier.create(transferService.transfer("sender",
                        new TransferRequestDto("recipient", new BigDecimal("100.00"))))
                .expectErrorMatches(e -> e instanceof ResponseStatusException rse
                        && rse.getStatusCode() == HttpStatus.BAD_REQUEST
                        && rse.getReason() != null && rse.getReason().contains("Перевод не выполнен"))
                .verify();

        verify(depositHeadersSpec, times(2)).retrieve();
        verify(repository).save(any(TransferTransaction.class));
    }

    @Test
    void transfer_withdrawFails_propagatesErrorWithoutCompensation() {
        when(withdrawResponseSpec.bodyToMono(AccountDto.class))
                .thenReturn(Mono.error(new RuntimeException("insufficient balance")));

        StepVerifier.create(transferService.transfer("sender",
                        new TransferRequestDto("recipient", new BigDecimal("500.00"))))
                .expectError(RuntimeException.class)
                .verify();

        verify(depositHeadersSpec, times(0)).retrieve();
        verify(repository, times(0)).save(any());
    }
}
