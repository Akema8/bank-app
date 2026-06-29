package ru.yandex.practicum.accounts.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import reactor.test.StepVerifier;
import ru.yandex.practicum.accounts.TestContainersConfig;
import ru.yandex.practicum.common.kafka.dto.NotificationEvent;

import java.time.Duration;
import java.util.Map;

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

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    void send_publishesMessageToKafka() {
        StepVerifier.create(notificationEventProducer.send("alice", "Пополнение счёта: +100"))
                .verifyComplete();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, NotificationEvent.class.getName());

        try (Consumer<String, NotificationEvent> consumer =
                     new DefaultKafkaConsumerFactory<String, NotificationEvent>(consumerProps).createConsumer()) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "notifications");
            ConsumerRecords<String, NotificationEvent> records =
                    KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

            assertThat(records.count()).isEqualTo(1);
            var record = records.iterator().next();
            assertThat(record.key()).isEqualTo("alice");
            assertThat(record.value().login()).isEqualTo("alice");
            assertThat(record.value().message()).isEqualTo("Пополнение счёта: +100");
        }
    }
}
