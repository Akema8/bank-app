package ru.yandex.practicum.mybankfront.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureWireMock(port = 0)
@Import(GatewayConsumerIT.TestRestClientConfig.class)
@TestPropertySource(properties = {
        "spring.config.import=",
        "eureka.client.enabled=false",
        "gateway.url=http://localhost:${wiremock.server.port}",
        "auth.url=http://localhost:${wiremock.server.port}"
})
class GatewayConsumerIT {

    @TestConfiguration
    static class TestRestClientConfig {
        @Bean
        RestClient accountsRestClient(@Value("${wiremock.server.port}") int port) {
            return RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }

        @Bean
        RestClient publicGatewayRestClient(@Value("${wiremock.server.port}") int port) {
            return RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }

        @Bean
        RestClient authRestClient(@Value("${wiremock.server.port}") int port) {
            return RestClient.builder()
                    .baseUrl("http://localhost:" + port)
                    .build();
        }
    }

    @Autowired
    AccountsClient accountsClient;
    @Autowired
    CashClient cashClient;
    @Autowired
    TransferClient transferClient;

    private static final String ACCOUNT_JSON = """
            {"id":1,"login":"user1","name":"Иван Иванов","birthdate":"1990-01-01","balance":500.00}
            """;
    private static final String ACCOUNTS_LIST_JSON = """
            [{"login":"user1","name":"Иван Иванов"},{"login":"user2","name":"Мария"}]
            """;

    @BeforeEach
    void setup() {
        WireMock.reset();
    }

    @Test
    void getMe_sendsGetToAccountsMe() {
        stubFor(get(urlEqualTo("/accounts/me"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ACCOUNT_JSON)));

        AccountDto result = accountsClient.getMe();

        assertThat(result).isNotNull();
        assertThat(result.login()).isEqualTo("user1");
        assertThat(result.balance()).isEqualByComparingTo("500.00");
        WireMock.verify(getRequestedFor(urlEqualTo("/accounts/me")));
    }

    @Test
    void getAll_sendsGetToAccounts() {
        stubFor(get(urlEqualTo("/accounts"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ACCOUNTS_LIST_JSON)));

        List<?> result = accountsClient.getAll();

        assertThat(result).hasSize(2);
        WireMock.verify(getRequestedFor(urlEqualTo("/accounts")));
    }

    @Test
    void cashDeposit_sendsPostWithAmountToCashDeposit() {
        stubFor(post(urlEqualTo("/cash/deposit"))
                .withHeader("Content-Type", WireMock.containing("application/json"))
                .withRequestBody(WireMock.matchingJsonPath("$.amount"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ACCOUNT_JSON)));

        AccountDto result = cashClient.deposit(new BigDecimal("100.00"));

        assertThat(result.balance()).isEqualByComparingTo("500.00");
        WireMock.verify(postRequestedFor(urlEqualTo("/cash/deposit"))
                .withHeader("Content-Type", WireMock.containing("application/json"))
                .withRequestBody(WireMock.matchingJsonPath("$.amount", equalTo("100.00"))));
    }

    @Test
    void cashWithdraw_sendsPostWithAmountToCashWithdraw() {
        stubFor(post(urlEqualTo("/cash/withdraw"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ACCOUNT_JSON)));

        cashClient.withdraw(new BigDecimal("50.00"));

        WireMock.verify(postRequestedFor(urlEqualTo("/cash/withdraw"))
                .withRequestBody(WireMock.matchingJsonPath("$.amount", equalTo("50.00"))));
    }

    @Test
    void transfer_sendsPostWithToLoginAndAmount() {
        stubFor(post(urlEqualTo("/transfer"))
                .withHeader("Content-Type", WireMock.containing("application/json"))
                .withRequestBody(WireMock.matchingJsonPath("$.toLogin"))
                .withRequestBody(WireMock.matchingJsonPath("$.amount"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(ACCOUNT_JSON)));

        transferClient.transfer("user2", new BigDecimal("200.00"));

        WireMock.verify(postRequestedFor(urlEqualTo("/transfer"))
                .withRequestBody(WireMock.matchingJsonPath("$.toLogin", equalTo("user2")))
                .withRequestBody(WireMock.matchingJsonPath("$.amount", equalTo("200.00"))));
    }
}
