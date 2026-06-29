package ru.yandex.practicum.notifications.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.common.kafka.dto.NotificationEvent;

@Slf4j
@Component
public class NotificationConsumer {

    @KafkaListener(topics = "${kafka.topic.notifications}")
    public void consume(NotificationEvent event) {
        log.info("NOTIFY: login={}, message={}", event.login(), event.message());
    }
}
