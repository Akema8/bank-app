package ru.yandex.practicum.authserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.config.import=",
        "eureka.client.enabled=false",
        "auth.issuer-uri=http://localhost:9000",
        "bank-web.redirect-uri=http://localhost:8080/login/oauth2/code/bank-web"
})
class AuthServerApplicationTests {

	@Test
	void contextLoads() {
	}
}
