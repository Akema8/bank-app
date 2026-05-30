package ru.yandex.practicum.transfer;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

public abstract class TestContainersConfig {

    static final MySQLContainer<?> mysql;

    static {
        mysql = new MySQLContainer<>("mysql:8.4")
                .withDatabaseName("transfer")
                .withUsername("test")
                .withPassword("test");
        mysql.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:mysql://" + mysql.getHost() + ":" + mysql.getMappedPort(3306) + "/transfer");
        registry.add("spring.r2dbc.username", mysql::getUsername);
        registry.add("spring.r2dbc.password", mysql::getPassword);

        registry.add("spring.cloud.zookeeper.enabled", () -> "false");
        registry.add("spring.cloud.zookeeper.config.enabled", () -> "false");
        registry.add("spring.cloud.zookeeper.discovery.enabled", () -> "false");
        registry.add("spring.cloud.service-registry.auto-registration.enabled", () -> "false");

        registry.add("spring.security.oauth2.client.provider.transfer-client.token-uri",
                () -> "http://localhost/oauth/token");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> "http://localhost/jwks");
    }
}
