package ru.practicum.transfer.notifications;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import ru.practicum.transfer.clients.NotificationsClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        ru.practicum.transfer.config.KafkaProducerConfig.class,
        ru.practicum.transfer.clients.NotificationsClient.class
}, properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "notifications.topic=notifications-test",
        "notifications.enabled=true",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
@EmbeddedKafka(partitions = 1, topics = "notifications-test")
@DirtiesContext
class NotificationsProducerKafkaTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private NotificationsClient notificationsClient;

    private Consumer<String, NotificationEvent> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("transfer-test-group", "true", embeddedKafka);
        consumerProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, org.springframework.kafka.support.serializer.JsonDeserializer.class);
        consumerProps.put(org.springframework.kafka.support.serializer.JsonDeserializer.VALUE_DEFAULT_TYPE, NotificationEvent.class);
        consumerProps.put(org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumer = new DefaultKafkaConsumerFactory<String, NotificationEvent>(consumerProps).createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "notifications-test");
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void sendsTransferNotificationEventToKafka() {
        notificationsClient.sendTransferOut("user-3", "bob", new BigDecimal("15.50"), "RUB");

        ConsumerRecord<String, NotificationEvent> record = KafkaTestUtils.getSingleRecord(consumer, "notifications-test");
        NotificationEvent event = record.value();

        assertThat(event.userId()).isEqualTo("user-3");
        assertThat(event.type()).isEqualTo("TRANSFER_OUT");
        assertThat(event.title()).contains("15.50");
        assertThat(event.createdAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }
}
