package ru.yandex.practicum.mybankfront.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Bean
    RestClient accountsRestClient(
            @Value("${gateway.url}") String gatewayUrl,
            OAuth2AuthorizedClientManager authorizedClientManager) {
        OAuth2ClientHttpRequestInterceptor interceptor =
                new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        interceptor.setClientRegistrationIdResolver(request -> "bank-web");
        return RestClient.builder()
                .baseUrl(gatewayUrl)
                .requestInterceptor(interceptor)
                .build();
    }

    @Bean
    RestClient publicGatewayRestClient(@Value("${gateway.url}") String gatewayUrl) {
        return RestClient.builder()
                .baseUrl(gatewayUrl)
                .build();
    }

    @Bean
    RestClient authRestClient(@Value("${auth.url}") String authUrl) {
        return RestClient.builder()
                .baseUrl(authUrl)
                .build();
    }
}
