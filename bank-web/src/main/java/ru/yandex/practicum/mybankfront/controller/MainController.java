package ru.yandex.practicum.mybankfront.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mybankfront.client.AccountsClient;
import ru.yandex.practicum.mybankfront.client.CashClient;
import ru.yandex.practicum.mybankfront.controller.dto.CashAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final AccountsClient accountsClient;
    private final CashClient cashClient;

    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String getAccount(Model model, @AuthenticationPrincipal OAuth2User principal) {
        return loadAccount(model, principal);
    }

    @PostMapping("/account")
    public String editAccount(
            Model model,
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam String name,
            @RequestParam LocalDate birthdate
    ) {
        try {
            accountsClient.update(name, birthdate);
        } catch (Exception e) {
            model.addAttribute("errors", List.of(e.getMessage()));
        }
        return loadAccount(model, principal);
    }

    @PostMapping("/cash")
    public String editCash(
            Model model,
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam int value,
            @RequestParam CashAction action
    ) {
        try {
            BigDecimal amount = BigDecimal.valueOf(value);
            if (action == CashAction.PUT) {
                cashClient.deposit(amount);
            } else {
                cashClient.withdraw(amount);
            }
        } catch (Exception e) {
            model.addAttribute("errors", List.of(e.getMessage()));
        }
        return loadAccount(model, principal);
    }

    @PostMapping("/transfer")
    public String transfer(
            Model model,
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam int value,
            @RequestParam String login
    ) {
        model.addAttribute("errors", List.of("Сервис Transfer ещё не реализован"));
        return loadAccount(model, principal);
    }

    private String loadAccount(Model model, OAuth2User principal) {
        try {
            var account = accountsClient.getMe();
            var accounts = accountsClient.getAll();
            model.addAttribute("name", account.name());
            model.addAttribute("birthdate", account.birthdate().toString());
            model.addAttribute("sum", account.balance());
            model.addAttribute("accounts", accounts);
        } catch (Exception e) {
            model.addAttribute("errors", List.of(e.getMessage()));
            model.addAttribute("accounts", List.of());
        }
        return "main";
    }
}
