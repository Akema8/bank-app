package ru.yandex.practicum.mybankfront.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import ru.yandex.practicum.mybankfront.client.AccountsClient;
import ru.yandex.practicum.mybankfront.client.AuthServerClient;
import ru.yandex.practicum.mybankfront.config.SecurityConfig;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(RegisterController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.config.import=",
        "spring.security.oauth2.client.provider.bank-web.authorization-uri=http://localhost/auth",
        "spring.security.oauth2.client.provider.bank-web.token-uri=http://localhost/token",
        "spring.security.oauth2.client.provider.bank-web.user-info-uri=http://localhost/userinfo"
})
class RegisterControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AccountsClient accountsClient;
    @MockitoBean
    AuthServerClient authServerClient;

    private static final AccountDto ACCOUNT = new AccountDto(
            1L, "alice", "Alice Smith", LocalDate.of(1990, 1, 1), BigDecimal.ZERO);

    @Test
    void getRegister_publiclyAccessible_returns200() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void postRegister_success_redirectsToOAuth2Login() throws Exception {
        when(accountsClient.register(any())).thenReturn(ACCOUNT);
        doNothing().when(authServerClient).register(any(), any());

        mockMvc.perform(post("/register")
                        .param("login", "alice")
                        .param("password", "secret")
                        .param("name", "Alice Smith")
                        .param("birthdate", "1990-01-01")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth2/authorization/bank-web"));
    }

    @Test
    void postRegister_accountsServiceFails_returnsFormWithError() throws Exception {
        when(accountsClient.register(any()))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.CONFLICT, "Conflict", null, null, null));

        mockMvc.perform(post("/register")
                        .param("login", "alice")
                        .param("password", "secret")
                        .param("name", "Alice Smith")
                        .param("birthdate", "1990-01-01")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));

        verify(authServerClient, never()).register(any(), any());
    }

    @Test
    void postRegister_authServerFails_returnsFormWithError() throws Exception {
        when(accountsClient.register(any())).thenReturn(ACCOUNT);
        doThrow(HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "Bad Request", null, null, null))
                .when(authServerClient).register(any(), any());

        mockMvc.perform(post("/register")
                        .param("login", "alice")
                        .param("password", "secret")
                        .param("name", "Alice Smith")
                        .param("birthdate", "1990-01-01")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }
}
