package ru.yandex.practicum.accounts.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.accounts.TestContainersConfig;
import ru.yandex.practicum.common.kafka.NotificationEventProducer;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "spring.config.import=",
        "eureka.client.enabled=false"
})
class AccountControllerIT extends TestContainersConfig {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    AccountRepository accountRepository;

    @MockitoBean
    ReactiveJwtDecoder jwtDecoder;

    @MockitoBean
    NotificationEventProducer notificationEventProducer;

    @BeforeEach
    void setup() {
        when(notificationEventProducer.send(anyString(), anyString())).thenReturn(Mono.empty());
        accountRepository.deleteAll().block();
    }

    @Test
    void register_validBody_returns201() {
        webTestClient.post().uri("/accounts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"login":"alice","name":"Alice Smith","birthdate":"1990-01-01"}
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.login").isEqualTo("alice")
                .jsonPath("$.balance").isEqualTo(0);
    }

    @Test
    void register_duplicateLogin_returns409() {
        webTestClient.post().uri("/accounts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"login":"bob","name":"Bob","birthdate":"1990-01-01"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post().uri("/accounts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"login":"bob","name":"Bob 2","birthdate":"1990-01-01"}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void register_underage_returns400() {
        webTestClient.post().uri("/accounts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"login":"teen","name":"Teen","birthdate":"%s"}
                        """.formatted(LocalDate.now().minusYears(16)))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getMe_authenticated_returnsAccount() {
        webTestClient.post().uri("/accounts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"login":"carol","name":"Carol","birthdate":"1990-01-01"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        webTestClient.mutateWith(mockJwt().jwt(j -> j.subject("carol")))
                .get().uri("/accounts/me")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.login").isEqualTo("carol");
    }

    @Test
    void deposit_withScope_updatesBalance() {
        webTestClient.post().uri("/accounts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"login":"dave","name":"Dave","birthdate":"1990-01-01"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        webTestClient
                .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("SCOPE_accounts.write")))
                .post().uri("/accounts/dave/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"amount": 200}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(200);
    }

    @Test
    void deposit_withoutScope_returns403() {
        webTestClient.post().uri("/accounts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"login":"eve","name":"Eve","birthdate":"1990-01-01"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        webTestClient
                .mutateWith(mockJwt())
                .post().uri("/accounts/eve/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"amount": 100}
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void withdraw_insufficientFunds_returns400() {
        webTestClient.post().uri("/accounts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"login":"frank","name":"Frank","birthdate":"1990-01-01"}
                        """)
                .exchange()
                .expectStatus().isCreated();

        webTestClient
                .mutateWith(mockJwt().authorities(new SimpleGrantedAuthority("SCOPE_accounts.write")))
                .post().uri("/accounts/frank/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"amount": 500}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
