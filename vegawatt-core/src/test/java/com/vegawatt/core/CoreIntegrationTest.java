package com.vegawatt.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.vegawatt.core.common.config.VegaWattKafkaProperties;
import com.vegawatt.core.common.outbox.OutboxEvent;
import com.vegawatt.core.common.outbox.OutboxRelayScheduler;
import com.vegawatt.core.common.outbox.OutboxRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

class CoreIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private OutboxRelayScheduler outboxRelayScheduler;

    @Autowired
    private VegaWattKafkaProperties kafkaProperties;

    @Test
    void contextLoadsWithEverySchedulerBeanResolved() {
        // Reaching this line means Flyway applied every migration against real Postgres, validate
        // matched the entities to that schema, and each @Scheduled(scheduler = "...") named a bean
        // that exists. Asserting the relay bean is present stops the test passing on an empty context.
        assertThat(outboxRelayScheduler).isNotNull();
    }

    @Test
    void relaysAnUnpublishedOutboxEventToTheRegistrationTopic() {
        UUID aggregateId = UUID.randomUUID();
        String payload = "{\"homeId\":\"" + aggregateId + "\",\"marker\":\"integration\"}";
        outboxRepository.save(OutboxEvent.create("HOME", aggregateId, "ASSET_REGISTERED", payload, Instant.now()));

        try (Consumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of(kafkaProperties.registrationTopic()));
            ConsumerRecord<String, String> record = awaitRecordWithKey(consumer, aggregateId.toString());
            assertThat(record.value()).isEqualTo(payload);
        }

        // The relay must also mark the row published, or the next scheduler tick would resend it.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(outboxRepository.findUnpublished(50))
                        .noneMatch(event -> event.aggregateId().equals(aggregateId)));
    }

    private Consumer<String, String> testConsumer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "integration-test-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new StringDeserializer())
                .createConsumer();
    }

    // I keep the first matching record across polls because the subscription rebalances on the first
    // poll and the message may only appear a poll or two later. Filtering by the unique aggregate id
    // keeps this from matching another test's events on the shared, compacted registration topic.
    private static ConsumerRecord<String, String> awaitRecordWithKey(Consumer<String, String> consumer, String key) {
        AtomicReference<ConsumerRecord<String, String>> found = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                if (key.equals(record.key())) {
                    found.set(record);
                }
            }
            assertThat(found.get()).isNotNull();
        });
        return found.get();
    }
}
