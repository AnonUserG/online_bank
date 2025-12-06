package ru.practicum.accounts.notifications;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import ru.practicum.accounts.clients.NotificationsClient;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationsClientMockProducerTest {

    @Test
    void sendsEventThroughMockProducer() {
        MockProducer<String, NotificationEvent> mockProducer =
                new MockProducer<>(true, new StringSerializer(), new JsonSerializer<>());
        ProducerFactory<String, NotificationEvent> pf = new DefaultKafkaProducerFactory<>(Collections.emptyMap()) {
            @Override
            public org.apache.kafka.clients.producer.Producer<String, NotificationEvent> createProducer() {
                return mockProducer;
            }
        };
        KafkaTemplate<String, NotificationEvent> template = new KafkaTemplate<>(pf);

        NotificationsClient client = new NotificationsClient(template, "notifications-test", true);

        client.sendRegistrationCompleted("user-mock");

        assertThat(mockProducer.history()).hasSize(1);
        ProducerRecord<String, NotificationEvent> record = mockProducer.history().getFirst();
        assertThat(record.topic()).isEqualTo("notifications-test");
        assertThat(record.key()).isEqualTo("user-mock");
        assertThat(record.value().type()).isEqualTo("ACCOUNT_REGISTERED");
    }
}
