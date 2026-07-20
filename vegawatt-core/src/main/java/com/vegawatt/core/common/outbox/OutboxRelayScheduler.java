package com.vegawatt.core.common.outbox;

import com.vegawatt.core.common.config.VegaWattKafkaProperties;
import com.vegawatt.core.common.time.ClockProvider;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public OutboxRelayScheduler(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
                                 ClockProvider clockProvider, VegaWattKafkaProperties kafkaProperties) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.clockProvider = clockProvider;
        this.kafkaProperties = kafkaProperties;
    }

    @Scheduled(fixedDelayString = "${vegawatt.outbox.relay-interval-ms}")
    public void relayPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findUnpublished(BATCH_SIZE);
        for (OutboxEvent event : pending) {
            relay(event);
        }
    }

    private void relay(OutboxEvent event) {
        String topic = resolveTopic(event.eventType());
        try {
            kafkaTemplate.send(topic, event.aggregateId().toString(), event.payload()).get();
            outboxRepository.save(event.published(clockProvider.now()));
        } catch (Exception e) {
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
