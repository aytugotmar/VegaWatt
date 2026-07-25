package com.vegawatt.core.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.config.VegaWattKafkaProperties;
import com.vegawatt.core.common.time.ClockProvider;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class OutboxRelaySchedulerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final long SHORT_TIMEOUT_MS = 150;
    private static final long DEAD_LETTER_AFTER_HOURS = 24;

    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private ClockProvider clockProvider;

    private OutboxRelayScheduler scheduler() {
        return new OutboxRelayScheduler(outboxRepository, kafkaTemplate, clockProvider,
                new VegaWattKafkaProperties("vegawatt.asset-registration.v1", "vegawatt.telemetry.v1"),
                SHORT_TIMEOUT_MS, DEAD_LETTER_AFTER_HOURS);
    }

    private static OutboxEvent pendingRegistration() {
        return OutboxEvent.create("HOME", UUID.randomUUID(), "ASSET_REGISTERED", "{}", NOW);
    }

    // The @Timeout is what makes this test useful rather than dangerous. Remove the bound from
    // the production get() and the relay blocks forever, which without this annotation hangs the
    // whole build instead of reporting a failure. A regression should go red, not go quiet.
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void givesUpOnASendThatNeverCompletesInsteadOfBlockingForever() {
        OutboxEvent event = pendingRegistration();
        when(outboxRepository.findUnpublished(anyInt())).thenReturn(List.of(event));
        when(clockProvider.now()).thenReturn(NOW);
        // A broker that accepts the connection and then never acknowledges. Before the timeout
        // existed this call parked the relay's only thread indefinitely, so a Kafka outage
        // stopped registration delivery entirely rather than retrying it.
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(new CompletableFuture<>());

        scheduler().relayPendingEvents();

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        org.mockito.Mockito.verify(outboxRepository).save(saved.capture());
        assertThat(saved.getValue().isPublished()).isFalse();
        assertThat(saved.getValue().retryCount()).isEqualTo(1);
        assertThat(saved.getValue().deadLettered()).isFalse();
    }

    @Test
    void keepsRetryingAFreshEventNoMatterHowManyAttemptsItHasHadSoFar() {
        // Dead-lettering is age-based, not attempt-count-based: a Kafka outage lasting a routine
        // 30-60s broker restart must not cost the event permanently just because the relay's
        // 2s interval let a lot of attempts pile up in that short a window. Even a large retryCount
        // must not dead-letter an event that's still well within the dead-letter-after-hours window.
        OutboxEvent manyAttemptsButStillRecent = new OutboxEvent(UUID.randomUUID(), "HOME", UUID.randomUUID(),
                "ASSET_REGISTERED", "{}", NOW, null, 500, false);
        when(outboxRepository.findUnpublished(anyInt())).thenReturn(List.of(manyAttemptsButStillRecent));
        when(clockProvider.now()).thenReturn(NOW.plusSeconds(60));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(new CompletableFuture<>());

        scheduler().relayPendingEvents();

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        org.mockito.Mockito.verify(outboxRepository).save(saved.capture());
        assertThat(saved.getValue().deadLettered()).isFalse();
        assertThat(saved.getValue().retryCount()).isEqualTo(501);
    }

    @Test
    void deadLettersAnEventOnceItsAgeExceedsTheConfiguredThresholdRegardlessOfAttemptCount() {
        // Only one prior attempt, but it's been failing for longer than dead-letter-after-hours —
        // this is the "genuinely stuck, not just a transient outage" case that should be flagged.
        OutboxEvent stuckForTooLong = new OutboxEvent(UUID.randomUUID(), "HOME", UUID.randomUUID(),
                "ASSET_REGISTERED", "{}", NOW, null, 1, false);
        when(outboxRepository.findUnpublished(anyInt())).thenReturn(List.of(stuckForTooLong));
        when(clockProvider.now()).thenReturn(NOW.plus(DEAD_LETTER_AFTER_HOURS + 1, ChronoUnit.HOURS));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(new CompletableFuture<>());

        scheduler().relayPendingEvents();

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        org.mockito.Mockito.verify(outboxRepository).save(saved.capture());
        assertThat(saved.getValue().deadLettered()).isTrue();
        assertThat(saved.getValue().isPublished()).isFalse();
    }

    @Test
    void marksTheEventPublishedWhenTheSendSucceeds() {
        OutboxEvent event = pendingRegistration();
        when(outboxRepository.findUnpublished(anyInt())).thenReturn(List.of(event));
        when(clockProvider.now()).thenReturn(NOW);
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture((SendResult<String, String>) null));

        scheduler().relayPendingEvents();

        ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
        org.mockito.Mockito.verify(outboxRepository).save(saved.capture());
        assertThat(saved.getValue().isPublished()).isTrue();
        assertThat(saved.getValue().publishedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().retryCount()).isZero();
    }
}
