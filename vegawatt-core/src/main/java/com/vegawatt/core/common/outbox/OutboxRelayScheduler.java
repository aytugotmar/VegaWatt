package com.vegawatt.core.common.outbox;

import com.vegawatt.core.common.config.VegaWattKafkaProperties;
import com.vegawatt.core.common.time.ClockProvider;
import java.time.Duration;
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
    private final Duration deadLetterAfter;

    public OutboxRelayScheduler(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate,
                                 ClockProvider clockProvider, VegaWattKafkaProperties kafkaProperties,
                                 @Value("${vegawatt.outbox.send-timeout-ms:10000}") long sendTimeoutMs,
                                 @Value("${vegawatt.outbox.dead-letter-after-hours:24}") long deadLetterAfterHours) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.clockProvider = clockProvider;
        this.kafkaProperties = kafkaProperties;
        this.sendTimeoutMs = sendTimeoutMs;
        this.deadLetterAfter = Duration.ofHours(deadLetterAfterHours);
    }

    @Scheduled(fixedDelayString = "${vegawatt.outbox.relay-interval-ms}", scheduler = "outboxTaskScheduler")
    public void relayPendingEvents() {
        List<OutboxEvent> pending = outboxRepository.findUnpublished(BATCH_SIZE);
        for (OutboxEvent event : pending) {
            relay(event);
        }
    }

    private void relay(OutboxEvent event) {
        String topic;
        try {
            topic = resolveTopic(event.eventType());
        } catch (UnknownOutboxEventTypeException e) {
            // Not transient — no retry will ever resolve an event type this code doesn't know
            // about — and it must not propagate out of this method: an uncaught exception here
            // would abort relayPendingEvents()'s for-loop entirely, leaving every event after
            // this one in the batch unprocessed for the whole tick, indefinitely (since this
            // event stays un-dead-lettered and un-published, it would be refetched and hit the
            // same throw on every subsequent tick too).
            log.error("Dead-lettering outbox event {} — unrecognized event type, will never resolve by retrying",
                    event.id(), e);
            outboxRepository.save(event.retryFailed().deadLetter());
            return;
        }
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
            handleFailure(event, topic, e);
        }
    }

    private void handleFailure(OutboxEvent event, String topic, Exception e) {
        int nextRetryCount = event.retryCount() + 1;
        // Age-based, not attempt-count-based: a fixed attempt count times a fixed relay interval
        // (e.g. 10 attempts x 2s = ~20s) dead-letters an event well before a routine Kafka broker
        // restart (which can easily take 30-60s) even finishes, permanently losing it. A Kafka
        // outage is transient on the timescale of minutes, not seconds, so retrying is cheap and
        // correct; only an event stuck for genuinely implausible-as-transient duration (default
        // 24h) is worth flagging as needing operator attention.
        Duration age = Duration.between(event.createdAt(), clockProvider.now());
        if (age.compareTo(deadLetterAfter) >= 0) {
            log.error("Dead-lettering outbox event {} (type={}) to topic {} after {} failed attempts over {}",
                    event.id(), event.eventType(), topic, nextRetryCount, age, e);
            outboxRepository.save(event.retryFailed().deadLetter());
            return;
        }
        log.error("Failed to relay outbox event {} (type={}) to topic {}, retryCount={}, age={}", event.id(),
                event.eventType(), topic, nextRetryCount, age, e);
        outboxRepository.save(event.retryFailed());
    }

    private String resolveTopic(String eventType) {
        return switch (eventType) {
            case "ASSET_REGISTERED" -> kafkaProperties.registrationTopic();
            default -> throw new UnknownOutboxEventTypeException(eventType);
        };
    }
}
