package ru.yandex.practicum.common.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.common.kafka.dto.NotificationEvent;

@Slf4j
@RequiredArgsConstructor
public class NotificationEventProducer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @Value("${kafka.topic.notifications}")
    private String topic;

    public Mono<Void> send(String login, String message) {
        return Mono.fromFuture(() -> kafkaTemplate.send(topic, login, new NotificationEvent(login, message)))
                .doOnError(e -> log.warn("Failed to send notification for {}: {}", login, e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .then();
    }
}
