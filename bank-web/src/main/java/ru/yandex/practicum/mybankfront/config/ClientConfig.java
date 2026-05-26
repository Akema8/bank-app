package ru.yandex.practicum.mybankfront.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Bean
    RestClient restClient(@Value("${accounts.url}") String accountsUrl) {
        return RestClient.builder()
                .baseUrl(accountsUrl)
                .build();
    }
}
