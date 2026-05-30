package ru.yandex.practicum.authserver.endpoint;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.authserver.registry.UserRegistry;

import java.util.List;

@Component
@Endpoint(id = "users")
@RequiredArgsConstructor
public class UsersEndpoint {

    private final UserRegistry userRegistry;

    @ReadOperation
    public List<String> users() {
        return userRegistry.getAll().stream().sorted().toList();
    }
}
