package ru.yandex.practicum.accounts.contract;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import ru.yandex.practicum.accounts.TestContainersConfig;
import ru.yandex.practicum.accounts.client.NotificationsClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest
@Testcontainers
@AutoConfigureWireMock(port = 0)
@Import(NotificationsConsumerIT.WireMockWebClientConfig.class)
@TestPropertySource(properties = {
        "spring.config.import=",
        "eureka.client.enabled=false"
})
class NotificationsConsumerIT extends TestContainersConfig {

    @Autowired
    NotificationsClient notificationsClient;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @TestConfiguration
    static class WireMockWebClientConfig {
        @Bean
        WebClient notificationsWebClient(@Value("${wiremock.server.port}") int wireMockPort) {
            return WebClient.builder()
                    .baseUrl("http://localhost:" + wireMockPort)
                    .build();
        }
    }

    @Test
    void notify_matchesNotificationsContract_returns202() {
        stubFor(post(urlEqualTo("/notifications"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.login"))
                .withRequestBody(matchingJsonPath("$.message"))
                .willReturn(aResponse().withStatus(202)));

        StepVerifier.create(notificationsClient.notify("user1", "Пополнение счёта: +100"))
                .verifyComplete();

        WireMock.verify(postRequestedFor(urlEqualTo("/notifications"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.login", equalTo("user1")))
                .withRequestBody(matchingJsonPath("$.message", containing("Пополнение"))));
    }
}
