package ru.yandex.practicum.accounts.contract;

import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.accounts.TestContainersConfig;
import ru.yandex.practicum.common.kafka.NotificationEventProducer;
import ru.yandex.practicum.accounts.model.Account;
import ru.yandex.practicum.accounts.repository.AccountRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "spring.config.import=",
        "eureka.client.enabled=false"
})
public abstract class AccountContractBase extends TestContainersConfig {

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
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("cash-client")
                .claim("scope", "accounts.write")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));
        when(notificationEventProducer.send(anyString(), anyString())).thenReturn(Mono.empty());

        accountRepository.deleteAll()
                .then(accountRepository.save(new Account(
                        null, "user1", "Иван Иванов",
                        LocalDate.of(1990, 1, 1),
                        new BigDecimal("500.00"))))
                .block();

        RestAssuredWebTestClient.webTestClient(webTestClient);
    }
}
