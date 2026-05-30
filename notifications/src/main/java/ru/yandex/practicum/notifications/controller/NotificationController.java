package ru.yandex.practicum.notifications.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.notifications.dto.NotificationDto;

@Slf4j
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void notify(@RequestBody NotificationDto dto) {
        log.info("NOTIFY: login={}, message={}", dto.login(), dto.message());
    }
}
