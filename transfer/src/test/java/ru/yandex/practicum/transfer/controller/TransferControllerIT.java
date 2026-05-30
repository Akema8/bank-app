package ru.yandex.practicum.transfer.controller;

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
import ru.yandex.practicum.transfer.TestContainersConfig;
import ru.yandex.practicum.transfer.repository.TransferTransactionRepository;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 9561)
@ActiveProfiles("test")
@Import(TransferControllerIT.TestWebClientConfig.class)
@TestPropertySource(properties = {
        "spring.config.import=",
        "eureka.client.enabled=false"
})
class TransferControllerIT extends TestContainersConfig {

    @TestConfiguration
    static class TestWebClientConfig {
        @Bean
        WebClient accountsWebClient() {
            return WebClient.builder().baseUrl("http://localhost:9561").build();
        }

        @Bean
        WebClient notificationsWebClient() {
            return WebClient.builder().baseUrl("http://localhost:9561").build();
        }
    }

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    TransferTransactionRepository repository;

    @MockitoBean
    ReactiveJwtDecoder jwtDecoder;

    private static final String SENDER_ACCOUNT = """
            {"id":1,"login":"sender","name":"Иван","birthdate":"1990-01-01","balance":400.00}
            """;
    private static final String RECIPIENT_ACCOUNT = """
            {"id":2,"login":"recipient","name":"Мария","birthdate":"1992-05-15","balance":600.00}
            """;

    @BeforeEach
    void setup() {
        repository.deleteAll().block();
        WireMock.reset();
        stubFor(post("/notifications").willReturn(aResponse().withStatus(202)));
    }

    @Test
    void transfer_success_returns200AndSavesRecord() {
        stubFor(post(urlPathMatching("/accounts/sender/withdraw"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(SENDER_ACCOUNT)));
        stubFor(post(urlPathMatching("/accounts/recipient/deposit"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(RECIPIENT_ACCOUNT)));

        webTestClient
                .mutateWith(mockJwt()
                        .jwt(j -> j.subject("sender"))
                        .authorities(new SimpleGrantedAuthority("SCOPE_transfer.write")))
                .post().uri("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"toLogin":"recipient","amount":100}
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.login").isEqualTo("sender");

        Long count = repository.count().block();
        assert count != null && count == 1;
    }

    @Test
    void transfer_depositFails_compensatesAndReturns400() {
        stubFor(post(urlPathMatching("/accounts/sender/withdraw"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(SENDER_ACCOUNT)));
        stubFor(post(urlPathMatching("/accounts/recipient/deposit"))
                .willReturn(aResponse().withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":400,\"error\":\"Bad Request\"}")));
        stubFor(post(urlPathMatching("/accounts/sender/deposit"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(SENDER_ACCOUNT)));

        webTestClient
                .mutateWith(mockJwt()
                        .jwt(j -> j.subject("sender"))
                        .authorities(new SimpleGrantedAuthority("SCOPE_transfer.write")))
                .post().uri("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"toLogin":"recipient","amount":100}
                        """)
                .exchange()
                .expectStatus().isBadRequest();

        // Compensation deposit was called
        WireMock.verify(postRequestedFor(urlEqualTo("/accounts/sender/deposit")));
        // FAILED record saved
        Long count = repository.count().block();
        assert count != null && count == 1;
    }

    @Test
    void transfer_selfTransfer_returns400() {
        webTestClient
                .mutateWith(mockJwt()
                        .jwt(j -> j.subject("user1"))
                        .authorities(new SimpleGrantedAuthority("SCOPE_transfer.write")))
                .post().uri("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"toLogin":"user1","amount":100}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void transfer_withoutScope_returns403() {
        webTestClient
                .mutateWith(mockJwt())
                .post().uri("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"toLogin":"recipient","amount":100}
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void transfer_unauthenticated_returns401() {
        webTestClient
                .post().uri("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"toLogin":"recipient","amount":100}
                        """)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void transfer_invalidBody_returns400() {
        webTestClient
                .mutateWith(mockJwt()
                        .jwt(j -> j.subject("sender"))
                        .authorities(new SimpleGrantedAuthority("SCOPE_transfer.write")))
                .post().uri("/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"toLogin":"","amount":-50}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
