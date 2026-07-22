package com.vegawatt.core.common.outbox;

import com.vegawatt.core.common.config.VegaWattKafkaProperties;
import com.vegawatt.core.common.time.ClockProvider;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxRelayScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayScheduler.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ClockProvider clockProvider;
    private final VegaWattKafkaProperties kafkaProperties;
    private final long sendTimeoutMs;

    public OutboxRelayScheduler(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
                                 ClockProvider clockProvider, VegaWattKafkaProperties kafkaProperties,
                                 @Value("${vegawatt.outbox.send-timeout-ms:10000}") long sendTimeoutMs) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.clockProvider = clockProvider;
        this.kafkaProperties = kafkaProperties;
        this.sendTimeoutMs = sendTimeoutMs;
    }

    @Scheduled(fixedDelayString = "${vegawatt.outbox.relay-interval-ms}", scheduler = "outboxTaskScheduler")
    public void relayPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findUnpublished(BATCH_SIZE);
        for (OutboxEvent event : pending) {
            relay(event);
        }
    }

    private void relay(OutboxEvent event) {
        String topic = resolveTopic(event.eventType());
        try {
            // Bounded on purpose. An unbounded get() waits for as long as the broker is
            // unreachable, and it does so holding this scheduler's only thread, so a Kafka
            // outage would stop the relay entirely rather than making it retry.
            //
            // A send that times out here may still land afterwards, since timing out does not
            // cancel it. That is safe for the registration topic specifically: it is keyed by
            // homeId and the sensor side upserts, so a duplicate delivery converges rather than
            // double-counting. Any future non-idempotent topic needs more than this.
            kafkaTemplate.send(topic, event.aggregateId().toString(), event.payload())
                    .get(sendTimeoutMs, TimeUnit.MILLISECONDS);
            outboxRepository.save(event.published(clockProvider.now()));
        } catch (InterruptedException e) {
            // Restore the flag so shutdown is not swallowed here, then leave the row unpublished
            // for the next run.
            Thread.currentThread().interrupt();
            log.warn("Interrupted while relaying outbox event {} to topic {}", event.id(), topic);
            outboxRepository.save(event.retryFailed());
        } catch (ExecutionException | TimeoutException e) {
            log.error("Failed to relay outbox event {} (type={}) to topic {}, retryCount={}", event.id(),
                    event.eventType(), topic, event.retryCount() + 1, e);
            outboxRepository.save(event.retryFailed());
        }
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case "ASSET_REGISTERED" -> kafkaProperties.registrationTopic();
            default -> throw new IllegalStateException("Unknown outbox event type: " + eventType);
        };
    }
}
