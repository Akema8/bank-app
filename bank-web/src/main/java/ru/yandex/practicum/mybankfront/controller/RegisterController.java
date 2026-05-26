package ru.yandex.practicum.mybankfront.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import ru.yandex.practicum.mybankfront.client.AccountsClient;
import ru.yandex.practicum.mybankfront.controller.dto.AccountRegisterDto;

import java.time.LocalDate;

@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterController {

    private final AccountsClient accountsClient;

    @GetMapping
    public String showForm() {
        return "register";
    }

    @PostMapping
    public String register(
            @RequestParam String login,
            @RequestParam String name,
            @RequestParam LocalDate birthdate,
            HttpSession session,
            Model model
    ) {
        try {
            accountsClient.register(new AccountRegisterDto(login, name, birthdate));
            session.setAttribute("login", login);
            return "redirect:/account";
        } catch (HttpClientErrorException e) {
            model.addAttribute("error", e.getResponseBodyAsString());
            model.addAttribute("login", login);
            model.addAttribute("name", name);
            model.addAttribute("birthdate", birthdate);
            return "register";
        }
    }
}
