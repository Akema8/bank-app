package ru.yandex.practicum.mybankfront.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mybankfront.client.AccountsClient;
import ru.yandex.practicum.mybankfront.controller.dto.CashAction;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final AccountsClient accountsClient;

    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    /**
     * GET /account.
     * TODO: логин получать из JWT (@AuthenticationPrincipal) после подключения OAuth2.
     */
    @GetMapping("/account")
    public String getAccount(Model model, HttpSession session) {
        String login = (String) session.getAttribute("login");
        if (login == null) {
            return "redirect:/register";
        }
        try {
            var account = accountsClient.getByLogin(login);
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

    /**
     * POST /account.
     * TODO: логин получать из JWT (@AuthenticationPrincipal) после подключения OAuth2.
     */
    @PostMapping("/account")
    public String editAccount(
            Model model,
            HttpSession session,
            @RequestParam("name") String name,
            @RequestParam("birthdate") LocalDate birthdate
    ) {
        String login = (String) session.getAttribute("login");
        if (login == null) {
            return "redirect:/register";
        }
        try {
            accountsClient.update(login, name, birthdate);
        } catch (Exception e) {
            model.addAttribute("errors", List.of(e.getMessage()));
        }
        return getAccount(model, session);
    }

    /**
     * POST /cash.
     * TODO: реализовать через сервис Cash после его создания.
     */
    @PostMapping("/cash")
    public String editCash(
            Model model,
            HttpSession session,
            @RequestParam("value") int value,
            @RequestParam("action") CashAction action
    ) {
        // TODO: вызвать cash-сервис через Gateway
        model.addAttribute("errors", List.of("Сервис Cash ещё не реализован"));
        return getAccount(model, session);
    }

    /**
     * POST /transfer.
     * TODO: реализовать через сервис Transfer после его создания.
     */
    @PostMapping("/transfer")
    public String transfer(
            Model model,
            HttpSession session,
            @RequestParam("value") int value,
            @RequestParam("login") String login
    ) {
        // TODO: вызвать transfer-сервис через Gateway
        model.addAttribute("errors", List.of("Сервис Transfer ещё не реализован"));
        return getAccount(model, session);
    }
}
