package ru.yandex.practicum.mybankfront.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;
import ru.yandex.practicum.mybankfront.client.AccountsClient;
import ru.yandex.practicum.mybankfront.client.CashClient;
import ru.yandex.practicum.mybankfront.client.TransferClient;
import ru.yandex.practicum.mybankfront.config.SecurityConfig;
import ru.yandex.practicum.mybankfront.controller.dto.AccountDto;
import ru.yandex.practicum.mybankfront.controller.dto.AccountShortDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(MainController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "spring.config.import=",
        "spring.security.oauth2.client.provider.bank-web.authorization-uri=http://localhost/auth",
        "spring.security.oauth2.client.provider.bank-web.token-uri=http://localhost/token",
        "spring.security.oauth2.client.provider.bank-web.user-info-uri=http://localhost/userinfo"
})
class MainControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AccountsClient accountsClient;
    @MockitoBean
    CashClient cashClient;
    @MockitoBean
    TransferClient transferClient;

    private static final AccountDto ACCOUNT = new AccountDto(
            1L, "user1", "Иван Иванов", LocalDate.of(1990, 1, 1), new BigDecimal("500.00"));

    @Test
    void getAccount_authenticated_returns200WithMainView() throws Exception {
        when(accountsClient.getMe()).thenReturn(ACCOUNT);
        when(accountsClient.getAll()).thenReturn(List.of(
                new AccountShortDto("user1", "Иван Иванов")));

        mockMvc.perform(get("/account").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("name", "sum", "accounts"));
    }

    @Test
    void getAccount_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/account"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/**"));
    }

    @Test
    void postCash_deposit_callsCashClientAndReturnsMain() throws Exception {
        when(accountsClient.getMe()).thenReturn(ACCOUNT);
        when(accountsClient.getAll()).thenReturn(List.of());
        when(cashClient.deposit(any())).thenReturn(ACCOUNT);

        mockMvc.perform(post("/cash")
                        .param("value", "100")
                        .param("action", "PUT")
                        .with(oauth2Login())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("main"));

        verify(cashClient).deposit(BigDecimal.valueOf(100));
    }

    @Test
    void postCash_cashClientThrows_addsErrorToModel() throws Exception {
        when(accountsClient.getMe()).thenReturn(ACCOUNT);
        when(accountsClient.getAll()).thenReturn(List.of());
        when(cashClient.withdraw(any())).thenThrow(new RestClientException("Insufficient funds"));

        mockMvc.perform(post("/cash")
                        .param("value", "9999")
                        .param("action", "GET")
                        .with(oauth2Login())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(model().attributeExists("errors"));
    }

    @Test
    void postTransfer_success_callsTransferClient() throws Exception {
        when(accountsClient.getMe()).thenReturn(ACCOUNT);
        when(accountsClient.getAll()).thenReturn(List.of());
        when(transferClient.transfer(anyString(), any())).thenReturn(ACCOUNT);

        mockMvc.perform(post("/transfer")
                        .param("value", "50")
                        .param("login", "user2")
                        .with(oauth2Login())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("main"));

        verify(transferClient).transfer("user2", BigDecimal.valueOf(50));
    }

    @Test
    void postAccount_updatesAccountAndReturnsMain() throws Exception {
        when(accountsClient.update(anyString(), any())).thenReturn(ACCOUNT);
        when(accountsClient.getMe()).thenReturn(ACCOUNT);
        when(accountsClient.getAll()).thenReturn(List.of());

        mockMvc.perform(post("/account")
                        .param("name", "Новое Имя")
                        .param("birthdate", "1990-01-01")
                        .with(oauth2Login())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("main"));

        verify(accountsClient).update("Новое Имя", LocalDate.of(1990, 1, 1));
    }
}
