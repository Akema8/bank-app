package ru.yandex.practicum.cash.contract;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.cash.TestContainersConfig;
import ru.yandex.practicum.cash.dto.AccountDto;
import ru.yandex.practicum.cash.kafka.NotificationEventProducer;
import ru.yandex.practicum.cash.dto.CashRequestDto;
import ru.yandex.practicum.cash.service.CashService;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWireMock(port = 9552)
@ActiveProfiles("test")
@Import(AccountsConsumerIT.TestWebClientConfig.class)
@TestPropertySource(properties = {
        "spring.config.import=",
        "eureka.client.enabled=false"
})
class AccountsConsumerIT extends TestContainersConfig {

    @TestConfiguration
    static class TestWebClientConfig {
        @Bean
        WebClient accountsWebClient() {
            return WebClient.builder().baseUrl("http://localhost:9552").build();
        }
    }

    @Autowired
    CashService cashService;

    @MockitoBean
    ReactiveJwtDecoder jwtDecoder;

    @MockitoBean
    NotificationEventProducer notificationEventProducer;

    @Test
    void deposit_matchesAccountsContract_sendsCorrectRequest() {
        stubFor(post(urlPathMatching("/accounts/.+/deposit"))
                .withHeader("Content-Type", WireMock.containing("application/json"))
                .withRequestBody(WireMock.matchingJsonPath("$.amount"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"id":1,"login":"user1","name":"Иван","birthdate":"1990-01-01","balance":600.00}
                                """)));

        when(notificationEventProducer.send(anyString(), anyString())).thenReturn(Mono.empty());

        StepVerifier.create(cashService.deposit("user1", new CashRequestDto(new BigDecimal("100.00"))))
                .assertNext(dto -> {
                    assertThat(dto).isInstanceOf(AccountDto.class);
                    assertThat(dto.login()).isEqualTo("user1");
                    assertThat(dto.balance()).isEqualByComparingTo("600.00");
                })
                .verifyComplete();

        WireMock.verify(postRequestedFor(urlEqualTo("/accounts/user1/deposit"))
                .withHeader("Content-Type", WireMock.containing("application/json"))
                .withRequestBody(WireMock.matchingJsonPath("$.amount", WireMock.equalTo("100.0"))));
    }
}
