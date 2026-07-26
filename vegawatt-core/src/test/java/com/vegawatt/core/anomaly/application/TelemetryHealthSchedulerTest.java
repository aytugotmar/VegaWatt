package com.vegawatt.core.anomaly.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.ApplianceHealthStatus;
import com.vegawatt.core.common.config.NotificationProperties;
import com.vegawatt.core.common.config.TelemetryHealthProperties;
import com.vegawatt.core.common.events.OperationalEvent;
import com.vegawatt.core.common.events.OperationalEventRepository;
import com.vegawatt.core.common.events.OperationalEventType;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.notification.domain.NotificationJob;
import com.vegawatt.core.notification.domain.NotificationJobRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelemetryHealthSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:10:00Z");
    private static final UUID HOME_ID = UUID.randomUUID();
    private static final TelemetryHealthProperties PROPERTIES = new TelemetryHealthProperties(30, 120, 15);

    @Mock
    private ApplianceLiveStatePort applianceLiveStatePort;
    @Mock
    private OperationalEventRepository operationalEventRepository;
    @Mock
    private NotificationJobRepository notificationJobRepository;
    @Mock
    private ClockProvider clockProvider;

    private TelemetryHealthTransitionRecorder transitionRecorder;
    private TelemetryHealthScheduler scheduler;

    @BeforeEach
    void setUp() {
        lenient().when(clockProvider.now()).thenReturn(NOW);
        transitionRecorder = new TelemetryHealthTransitionRecorder(operationalEventRepository, notificationJobRepository,
                new NotificationProperties(30));
        scheduler = new TelemetryHealthScheduler(applianceLiveStatePort, transitionRecorder, clockProvider, PROPERTIES);
        lenient().when(operationalEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static ApplianceLiveState liveState(UUID applianceId, ApplianceHealthStatus status,
                                                 Instant lastUpdatedAt) {
        return new ApplianceLiveState(HOME_ID, applianceId, "TV", "TELEVISION", new BigDecimal("180"),
                new BigDecimal("1"), null, null, BigDecimal.ZERO.setScale(9), 0, 0, false, 0, 0, false, status,
                lastUpdatedAt, null, 0L, 0L);
    }

    @SuppressWarnings("unchecked")
    private void stubUpdateToApply(UUID applianceId, ApplianceLiveState existing) {
        when(applianceLiveStatePort.update(eq(HOME_ID), eq(applianceId), any())).thenAnswer(invocation -> {
            UnaryOperator<ApplianceLiveState> mutator = invocation.getArgument(2);
            return mutator.apply(existing);
        });
    }

    @Test
    void freshApplianceIsUntouched() {
        UUID applianceId = UUID.randomUUID();
        when(applianceLiveStatePort.getAll())
                .thenReturn(List.of(liveState(applianceId, ApplianceHealthStatus.NORMAL, NOW.minusSeconds(5))));

        scheduler.sweep();

        verify(applianceLiveStatePort, never()).update(any(), any(), any());
        verify(operationalEventRepository, never()).save(any());
    }

    @Test
    void transitionsToStaleAfterThresholdAndEmitsEventAndJob() {
        UUID applianceId = UUID.randomUUID();
        ApplianceLiveState existing = liveState(applianceId, ApplianceHealthStatus.NORMAL, NOW.minusSeconds(45));
        when(applianceLiveStatePort.getAll()).thenReturn(List.of(existing));
        stubUpdateToApply(applianceId, existing);

        scheduler.sweep();

        ArgumentCaptor<OperationalEvent> eventCaptor = ArgumentCaptor.forClass(OperationalEvent.class);
        verify(operationalEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(OperationalEventType.APPLIANCE_TELEMETRY_STALE);

        ArgumentCaptor<NotificationJob> jobCaptor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(notificationJobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().triggerType()).isEqualTo(AdvisoryTriggerType.TELEMETRY_STALE);
    }

    @Test
    void transitionsToOfflineAfterLongerThresholdAndEmitsEventAndJob() {
        UUID applianceId = UUID.randomUUID();
        ApplianceLiveState existing = liveState(applianceId, ApplianceHealthStatus.STALE, NOW.minusSeconds(150));
        when(applianceLiveStatePort.getAll()).thenReturn(List.of(existing));
        stubUpdateToApply(applianceId, existing);

        scheduler.sweep();

        ArgumentCaptor<OperationalEvent> eventCaptor = ArgumentCaptor.forClass(OperationalEvent.class);
        verify(operationalEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().eventType()).isEqualTo(OperationalEventType.APPLIANCE_TELEMETRY_OFFLINE);

        ArgumentCaptor<NotificationJob> jobCaptor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(notificationJobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().triggerType()).isEqualTo(AdvisoryTriggerType.TELEMETRY_OFFLINE);
    }

    @Test
    void alreadyOfflineApplianceIsNotRefiredOnSubsequentTicks() {
        UUID applianceId = UUID.randomUUID();
        when(applianceLiveStatePort.getAll())
                .thenReturn(List.of(liveState(applianceId, ApplianceHealthStatus.OFFLINE, NOW.minusSeconds(600))));

        scheduler.sweep();

        verify(applianceLiveStatePort, never()).update(any(), any(), any());
        verify(operationalEventRepository, never()).save(any());
        verify(notificationJobRepository, never()).save(any());
    }

    @Test
    void reCheckInsideTheTransactionSkipsAnApplianceThatRecoveredBetweenScanAndUpdate() {
        UUID applianceId = UUID.randomUUID();
        ApplianceLiveState scanned = liveState(applianceId, ApplianceHealthStatus.NORMAL, NOW.minusSeconds(45));
        ApplianceLiveState recovered = liveState(applianceId, ApplianceHealthStatus.NORMAL, NOW);
        when(applianceLiveStatePort.getAll()).thenReturn(List.of(scanned));
        stubUpdateToApply(applianceId, recovered);

        scheduler.sweep();

        verify(operationalEventRepository, never()).save(any());
        verify(notificationJobRepository, never()).save(any());
    }

    @Test
    void oneApplianceFailingDoesNotStopTheRestOfTheSweep() {
        UUID failingApplianceId = UUID.randomUUID();
        UUID transitioningApplianceId = UUID.randomUUID();
        ApplianceLiveState failing = liveState(failingApplianceId, ApplianceHealthStatus.NORMAL,
                NOW.minusSeconds(45));
        ApplianceLiveState transitioning = liveState(transitioningApplianceId, ApplianceHealthStatus.NORMAL,
                NOW.minusSeconds(45));
        when(applianceLiveStatePort.getAll()).thenReturn(List.of(failing, transitioning));
        when(applianceLiveStatePort.update(eq(HOME_ID), eq(failingApplianceId), any()))
                .thenThrow(new RuntimeException("ignite unavailable"));
        stubUpdateToApply(transitioningApplianceId, transitioning);

        scheduler.sweep();

        verify(operationalEventRepository).save(any());
        verify(notificationJobRepository).save(any());
    }

    @Test
    void databaseFailureTriggersCompensatingRollbackOfLiveState() {
        UUID applianceId = UUID.randomUUID();
        ApplianceLiveState existing = liveState(applianceId, ApplianceHealthStatus.NORMAL, NOW.minusSeconds(45));
        when(applianceLiveStatePort.getAll()).thenReturn(List.of(existing));
        stubUpdateToApply(applianceId, existing);
        when(operationalEventRepository.save(any())).thenThrow(new RuntimeException("PostgreSQL connection lost"));

        scheduler.sweep();

        // Second update call restores previous live state
        verify(applianceLiveStatePort, org.mockito.Mockito.times(2)).update(eq(HOME_ID), eq(applianceId), any());
    }

    @Test
    void doesNotClobberANewerWriteWhenCompensatingAfterADatabaseFailure() {
        UUID applianceId = UUID.randomUUID();
        ApplianceLiveState existing = liveState(applianceId, ApplianceHealthStatus.NORMAL, NOW.minusSeconds(45));
        when(applianceLiveStatePort.getAll()).thenReturn(List.of(existing));
        when(operationalEventRepository.save(any())).thenThrow(new RuntimeException("PostgreSQL connection lost"));

        // Simulates the real Ignite adapter's update(): the transition write lands at version 5.
        // Before the compensation runs, some other mutator (a telemetry event, another sweep) has
        // legitimately advanced the cache entry to version 6 — that write must survive.
        ApplianceLiveState supersededAtVersion6 =
                liveState(applianceId, ApplianceHealthStatus.NORMAL, NOW).withStateVersion(6L);
        AtomicReference<ApplianceLiveState> compensationResult = new AtomicReference<>();

        when(applianceLiveStatePort.update(eq(HOME_ID), eq(applianceId), any()))
                .thenAnswer(invocation -> {
                    UnaryOperator<ApplianceLiveState> mutator = invocation.getArgument(2);
                    return mutator.apply(existing).withStateVersion(5L);
                })
                .thenAnswer(invocation -> {
                    UnaryOperator<ApplianceLiveState> mutator = invocation.getArgument(2);
                    ApplianceLiveState result = mutator.apply(supersededAtVersion6);
                    compensationResult.set(result);
                    return result;
                });

        scheduler.sweep();

        verify(applianceLiveStatePort, org.mockito.Mockito.times(2)).update(eq(HOME_ID), eq(applianceId), any());
        assertThat(compensationResult.get())
                .as("a legitimately newer write (version 6) must survive a stale rollback targeting version 5")
                .isSameAs(supersededAtVersion6);
    }
}
