package ru.yandex.practicum.notifications.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "eureka.client.enabled=false")
class NotificationControllerIT {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private static final String VALID_BODY = """
            {"login":"user1","message":"Снятие со счёта: -50"}
            """;

    @Test
    void notify_withValidScope_fullStack_returns202() throws Exception {
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_notifications.write"))))
                .andExpect(status().isAccepted());
    }

    @Test
    void notify_withWrongScope_fullStack_returns403() throws Exception {
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY)
                        .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_cash.write"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void notify_unauthenticated_fullStack_returns401() throws Exception {
        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }
}