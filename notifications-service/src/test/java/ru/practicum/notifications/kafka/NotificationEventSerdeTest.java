package ru.practicum.notifications.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEventSerdeTest {

    @Test
    void mockProducerSendsEvent() {
        NotificationEvent event = NotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .userId("user-42")
                .type("TEST")
                .title("title")
                .content("content")
                .createdAt(OffsetDateTime.now())
                .build();

        MockProducer<String, NotificationEvent> mockProducer =
                new MockProducer<>(true, new StringSerializer(), new JsonSerializer<>());

        mockProducer.send(new ProducerRecord<>("notifications-test", event.userId(), event));

        assertThat(mockProducer.history()).hasSize(1);
        ProducerRecord<String, NotificationEvent> record = mockProducer.history().getFirst();
        assertThat(record.value()).isEqualTo(event);
        assertThat(record.key()).isEqualTo("user-42");
    }

    @Test
    void mockConsumerReadsEventWithDeserializer() {
        NotificationEvent event = NotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .userId("user-99")
                .type("TEST")
                .title("title")
                .content("content")
                .createdAt(OffsetDateTime.now())
                .build();

        JsonSerializer<NotificationEvent> serializer = new JsonSerializer<>();
        byte[] bytes = serializer.serialize("notifications-test", event);

        JsonDeserializer<NotificationEvent> deserializer = new JsonDeserializer<>(NotificationEvent.class);
        deserializer.addTrustedPackages("*");

        MockConsumer<String, NotificationEvent> consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
        TopicPartition tp = new TopicPartition("notifications-test", 0);
        consumer.assign(List.of(tp));
        consumer.updateBeginningOffsets(java.util.Map.of(tp, 0L));

        consumer.addRecord(new ConsumerRecord<>("notifications-test", 0, 0, "user-99",
                deserializer.deserialize("notifications-test", bytes)));

        var records = consumer.poll(java.time.Duration.ofMillis(100));
        assertThat(records.count()).isEqualTo(1);
        NotificationEvent received = records.iterator().next().value();
        assertThat(received.userId()).isEqualTo("user-99");
        assertThat(received.type()).isEqualTo("TEST");
    }
}
