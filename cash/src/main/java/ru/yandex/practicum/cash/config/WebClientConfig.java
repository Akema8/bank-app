package ru.yandex.practicum.cash.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    ReactiveOAuth2AuthorizedClientManager authorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ReactiveOAuth2AuthorizedClientService authorizedClientService) {
        var provider = ReactiveOAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();
        var manager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    @Bean
    @LoadBalanced
    WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    @Profile("!test")
    WebClient accountsWebClient(WebClient.Builder loadBalancedWebClientBuilder,
                                ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        return buildClient("lb://accounts", loadBalancedWebClientBuilder, authorizedClientManager);
    }

    @Bean
    @Profile("!test")
    WebClient notificationsWebClient(WebClient.Builder loadBalancedWebClientBuilder,
                                     ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        return buildClient("lb://notifications", loadBalancedWebClientBuilder, authorizedClientManager);
    }

    private WebClient buildClient(String baseUrl, WebClient.Builder builder,
                                  ReactiveOAuth2AuthorizedClientManager manager) {
        var oauth2 = new ServerOAuth2AuthorizedClientExchangeFilterFunction(manager);
        oauth2.setDefaultClientRegistrationId("cash-client");
        return builder.clone()
                .baseUrl(baseUrl)
                .filter(oauth2)
                .build();
    }
}
