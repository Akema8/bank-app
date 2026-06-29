package ru.yandex.practicum.common.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.common.kafka.dto.NotificationEvent;

@Slf4j
public class NotificationEventProducer {

    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${kafka.topic.notifications}")
    private String topic;

    public NotificationEventProducer(KafkaTemplate<String, NotificationEvent> kafkaTemplate,
                                     MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
    }

    public Mono<Void> send(String login, String message) {
        return Mono.fromFuture(() -> kafkaTemplate.send(topic, login, new NotificationEvent(login, message)))
                .doOnError(e -> {
                    log.warn("Failed to send notification for {}: {}", login, e.getMessage());
                    meterRegistry.counter("notification.send.failures", "login", login).increment();
                })
                .onErrorResume(e -> Mono.empty())
                .then();
    }
}
