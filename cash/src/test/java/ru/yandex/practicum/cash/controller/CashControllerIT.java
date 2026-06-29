package ru.yandex.practicum.cash.controller;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.cash.TestContainersConfig;
import ru.yandex.practicum.common.kafka.NotificationEventProducer;
import ru.yandex.practicum.cash.repository.CashTransactionRepository;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 9551)
@ActiveProfiles("test")
@Import(CashControllerIT.TestWebClientConfig.class)
@TestPropertySource(properties = {
        "spring.config.import=",
        "eureka.client.enabled=false"
})
class CashControllerIT extends TestContainersConfig {

    @TestConfiguration
    static class TestWebClientConfig {
        @Bean
        WebClient accountsWebClient() {
            return WebClient.builder().baseUrl("http://localhost:9551").build();
        }
    }

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    CashTransactionRepository repository;

    @MockitoBean
    ReactiveJwtDecoder jwtDecoder;

    @MockitoBean
    NotificationEventProducer notificationEventProducer;

    private static final String ACCOUNT_RESPONSE = """
            {"id":1,"login":"user1","name":"Иван","birthdate":"1990-01-01","balance":600.00}
            """;

    @BeforeEach
    void setup() {
        repository.deleteAll().block();
        WireMock.reset();
        when(notificationEventProducer.send(anyString(), anyString())).thenReturn(Mono.empty());

        stubFor(post(urlPathMatching("/accounts/.+/(deposit|withdraw)"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ACCOUNT_RESPONSE)));
    }

    @Test
    void deposit_withScope_returns200AndSavesTransaction() {
        webTestClient
                .mutateWith(mockJwt()
                        .jwt(j -> j.subject("user1"))
                        .authorities(new SimpleGrantedAuthority("SCOPE_cash.write")))
                .post().uri("/cash/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"amount": 100}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.login").isEqualTo("user1")
                .jsonPath("$.balance").isEqualTo(600.0);

        Long count = repository.count().block();
        assert count != null && count == 1;
    }

    @Test
    void withdraw_withScope_returns200AndSavesTransaction() {
        webTestClient
                .mutateWith(mockJwt()
                        .jwt(j -> j.subject("user1"))
                        .authorities(new SimpleGrantedAuthority("SCOPE_cash.write")))
                .post().uri("/cash/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"amount": 50}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.login").isEqualTo("user1");
    }

    @Test
    void deposit_withoutScope_returns403() {
        webTestClient
                .mutateWith(mockJwt())
                .post().uri("/cash/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"amount": 100}
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void deposit_unauthenticated_returns401() {
        webTestClient
                .post().uri("/cash/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"amount": 100}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deposit_invalidAmount_returns400() {
        webTestClient
                .mutateWith(mockJwt()
                        .jwt(j -> j.subject("user1"))
                        .authorities(new SimpleGrantedAuthority("SCOPE_cash.write")))
                .post().uri("/cash/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"amount": -50}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
