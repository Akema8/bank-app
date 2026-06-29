package ru.yandex.practicum.notifications.consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import ru.yandex.practicum.common.kafka.dto.NotificationEvent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "${kafka.topic.notifications:notifications}")
@TestPropertySource(properties = {
        "spring.config.import=",
        "eureka.client.enabled=false",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.properties.spring.json.add.type.headers=false",
        "kafka.topic.notifications=notifications"
})
class NotificationConsumerIT {

    @Autowired
    KafkaTemplate<Object, Object> kafkaTemplate;

    @MockitoSpyBean
    NotificationConsumer notificationConsumer;

    @Value("${kafka.topic.notifications}")
    String topic;

    @Test
    void consume_receivesMessageAndLogs() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> {
            inv.callRealMethod();
            latch.countDown();
            return null;
        }).when(notificationConsumer).consume(any(NotificationEvent.class));

        kafkaTemplate.send(topic, "user1", new NotificationEvent("user1", "Test message"));

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        verify(notificationConsumer).consume(new NotificationEvent("user1", "Test message"));
    }
}
