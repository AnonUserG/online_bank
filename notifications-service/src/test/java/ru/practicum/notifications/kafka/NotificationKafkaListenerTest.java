package ru.practicum.notifications.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.boot.test.mock.mockito.SpyBean;
import ru.practicum.notifications.service.NotificationService;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "app.kafka.notifications-topic=notifications-test",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
@EmbeddedKafka(partitions = 1, topics = "notifications-test")
@DirtiesContext
class NotificationKafkaListenerTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @SpyBean
    private NotificationService notificationService;

    private KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    @BeforeEach
    void setUp() {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));
    }

    @Test
    void consumesNotificationEventFromKafka() throws Exception {
        NotificationEvent event = NotificationEvent.builder()
                .eventId(UUID.randomUUID())
                .userId("user-123")
                .type("CASH_IN")
                .title("Пополнение счета")
                .content("Зачислено 100.00 RUB")
                .createdAt(OffsetDateTime.now())
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(invocation -> {
            latch.countDown();
            return invocation.callRealMethod();
        }).when(notificationService).accept(any(NotificationEvent.class));

        kafkaTemplate.send("notifications-test", event.userId(), event);

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        verify(notificationService, atLeastOnce()).accept(Mockito.argThat(received ->
                received.eventId().equals(event.eventId())
                        && received.userId().equals(event.userId())
                        && received.type().equals(event.type())
                        && received.title().equals(event.title())
                        && received.content().equals(event.content())
        ));
    }
}
