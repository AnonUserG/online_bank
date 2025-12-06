package ru.practicum.accounts.notifications;

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
import ru.practicum.accounts.clients.NotificationsClient;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        ru.practicum.accounts.config.KafkaProducerConfig.class,
        ru.practicum.accounts.clients.NotificationsClient.class
}, properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "notifications.enabled=true",
        "notifications.topic=notifications-test",
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
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafka);
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
    void sendsNotificationEventToKafka() {
        notificationsClient.sendRegistrationCompleted("user-1");

        ConsumerRecord<String, NotificationEvent> record = KafkaTestUtils.getSingleRecord(consumer, "notifications-test");
        NotificationEvent event = record.value();

        assertThat(event.userId()).isEqualTo("user-1");
        assertThat(event.type()).isEqualTo("ACCOUNT_REGISTERED");
        assertThat(event.title()).isEqualTo("Registration completed");
        assertThat(event.createdAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }
}
