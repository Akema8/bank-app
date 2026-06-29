package ru.yandex.practicum.common.kafka;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import ru.yandex.practicum.common.kafka.dto.NotificationEvent;

@AutoConfiguration(after = KafkaAutoConfiguration.class)
public class NotificationKafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NotificationEventProducer notificationEventProducer(KafkaProperties kafkaProperties) {
        var factory = new DefaultKafkaProducerFactory<String, NotificationEvent>(
                kafkaProperties.buildProducerProperties(null));
        return new NotificationEventProducer(new KafkaTemplate<>(factory));
    }
}
