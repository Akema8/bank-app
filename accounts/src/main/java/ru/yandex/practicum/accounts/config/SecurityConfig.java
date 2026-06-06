package ru.yandex.practicum.accounts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(auth -> auth
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers(HttpMethod.POST, "/accounts/register").permitAll()
                        .pathMatchers(HttpMethod.POST, "/accounts/*/deposit", "/accounts/*/withdraw")
                                .hasAuthority("SCOPE_accounts.write")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .build();
    }

    private Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> jwtAuthConverter() {
        JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> scopes = scopesConverter.convert(jwt);
            Object claim = jwt.getClaim("authorities");
            Stream<GrantedAuthority> extra = Stream.empty();
            if (claim instanceof Collection<?> c) {
                extra = c.stream().map(Object::toString).map(SimpleGrantedAuthority::new);
            }
            return Stream.concat(scopes == null ? Stream.empty() : scopes.stream(), extra).collect(Collectors.toSet());
        });
        return new ReactiveJwtAuthenticationConverterAdapter(delegate);
    }
}
