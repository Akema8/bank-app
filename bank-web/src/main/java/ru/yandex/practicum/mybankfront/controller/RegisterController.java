package ru.yandex.practicum.mybankfront.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;
import ru.yandex.practicum.mybankfront.client.AccountsClient;
import ru.yandex.practicum.mybankfront.client.AuthServerClient;
import ru.yandex.practicum.mybankfront.controller.dto.AccountRegisterDto;

import java.time.LocalDate;

@Slf4j
@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterController {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final AccountsClient accountsClient;
    private final AuthServerClient authServerClient;

    @GetMapping
    public String showForm() {
        return "register";
    }

    @PostMapping
    public String register(
            @RequestParam String login,
            @RequestParam String password,
            @RequestParam String name,
            @RequestParam LocalDate birthdate,
            Model model
    ) {
        log.info("REGISTER START: login={}, name={}, birthdate={}", login, name, birthdate);
        try {
            accountsClient.register(new AccountRegisterDto(login, name, birthdate));
            log.info("REGISTER accounts OK: login={}", login);
        } catch (Exception e) {
            log.error("REGISTER accounts FAILED: login={}", login, e);
            model.addAttribute("error", friendlyMessage(e));
            model.addAttribute("login", login);
            model.addAttribute("name", name);
            model.addAttribute("birthdate", birthdate);
            return "register";
        }

        try {
            authServerClient.register(login, password);
            log.info("REGISTER auth OK: login={}", login);
        } catch (Exception e) {
            log.error("REGISTER auth FAILED: login={}", login, e);
            model.addAttribute("error", friendlyMessage(e));
            model.addAttribute("login", login);
            model.addAttribute("name", name);
            model.addAttribute("birthdate", birthdate);
            return "register";
        }

        log.info("REGISTER ALL OK, redirecting to OAuth2: login={}", login);
        return "redirect:/oauth2/authorization/bank-web";
    }

    private static String friendlyMessage(Exception e) {
        if (e instanceof RestClientResponseException rce) {
            String body = rce.getResponseBodyAsString();
            if (body != null && !body.isBlank()) {
                try {
                    JsonNode node = JSON.readTree(body);
                    if (node.hasNonNull("message")) {
                        return node.get("message").asText();
                    }
                    if (node.hasNonNull("error")) {
                        return node.get("error").asText();
                    }
                } catch (Exception ignored) {
                }
                return body;
            }
            return rce.getStatusText();
        }
        return e.getMessage();
    }
}
