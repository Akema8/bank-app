package ru.yandex.practicum.accounts.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;
import ru.yandex.practicum.accounts.TestContainersConfig;
import ru.yandex.practicum.accounts.dto.NotificationEvent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "notifications")
@TestPropertySource(properties = {
        "spring.config.import=",
        "eureka.client.enabled=false",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "kafka.topic.notifications=notifications"
})
class NotificationEventProducerIT extends TestContainersConfig {

    @Autowired
    NotificationEventProducer notificationEventProducer;

    private final BlockingQueue<NotificationEvent> received = new LinkedBlockingQueue<>();

    @KafkaListener(topics = "notifications", groupId = "test-producer-group",
            properties = {
                    "spring.json.value.default.type=ru.yandex.practicum.accounts.dto.NotificationEvent",
                    "spring.json.trusted.packages=*"
            })
    void listen(NotificationEvent event) {
        received.add(event);
    }

    @Test
    void send_publishesMessageToKafka() throws Exception {
        StepVerifier.create(notificationEventProducer.send("alice", "Пополнение счёта: +100"))
                .verifyComplete();

        NotificationEvent event = received.poll(10, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event.login()).isEqualTo("alice");
        assertThat(event.message()).isEqualTo("Пополнение счёта: +100");
    }
}
