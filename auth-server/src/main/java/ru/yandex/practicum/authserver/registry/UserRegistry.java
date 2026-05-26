package ru.yandex.practicum.authserver.registry;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UserRegistry {

    private final Set<String> logins = ConcurrentHashMap.newKeySet();

    public void add(String login) {
        logins.add(login);
    }

    public Set<String> getAll() {
        return Collections.unmodifiableSet(logins);
    }
}
