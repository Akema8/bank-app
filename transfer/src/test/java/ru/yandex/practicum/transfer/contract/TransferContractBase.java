package ru.yandex.practicum.transfer.contract;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.transfer.TestContainersConfig;
import ru.yandex.practicum.transfer.repository.TransferTransactionRepository;

import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@AutoConfigureWireMock(port = 9562)
@ActiveProfiles("test")
@Import(TransferContractBase.TestWebClientConfig.class)
@TestPropertySource(properties = {
        "spring.config.import=",
        "eureka.client.enabled=false"
})
public abstract class TransferContractBase extends TestContainersConfig {

    @TestConfiguration
    static class TestWebClientConfig {
        @Bean
        WebClient accountsWebClient() {
            return WebClient.builder().baseUrl("http://localhost:9562").build();
        }

        @Bean
        WebClient notificationsWebClient() {
            return WebClient.builder().baseUrl("http://localhost:9562").build();
        }
    }

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    TransferTransactionRepository repository;

    @MockitoBean
    ReactiveJwtDecoder jwtDecoder;

    @BeforeEach
    void setup() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("sender")
                .claim("scope", "transfer.write")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));

        repository.deleteAll().block();
        WireMock.reset();

        stubFor(post(urlPathMatching("/accounts/sender/withdraw"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":1,"login":"sender","name":"Иван","birthdate":"1990-01-01","balance":400.00}
                                """)));
        stubFor(post(urlPathMatching("/accounts/recipient/deposit"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":2,"login":"recipient","name":"Мария","birthdate":"1992-05-15","balance":600.00}
                                """)));
        stubFor(post("/notifications")
                .willReturn(aResponse().withStatus(202)));

        RestAssuredWebTestClient.webTestClient(webTestClient);
    }
}
