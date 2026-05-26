package ru.yandex.practicum.authserver.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.yandex.practicum.authserver.dto.UserRegistrationDto;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserRegistrationController {

    private final InMemoryUserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody UserRegistrationDto dto) {
        if (userDetailsManager.userExists(dto.login())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Логин уже занят: " + dto.login());
        }
        userDetailsManager.createUser(
                User.builder()
                        .username(dto.login())
                        .password(passwordEncoder.encode(dto.password()))
                        .roles("USER")
                        .build()
        );
    }
}
