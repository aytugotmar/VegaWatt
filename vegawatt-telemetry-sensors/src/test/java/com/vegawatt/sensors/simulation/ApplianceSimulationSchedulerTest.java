package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.sensors.publishing.TelemetryEventPublisher;
import com.vegawatt.sensors.registration.ApplianceConfig;
import com.vegawatt.sensors.registration.HomeRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class ApplianceSimulationSchedulerTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private HomeRegistry homeRegistry;

    @Mock
    private RandomSource randomSource;

    @Mock
    private TelemetryEventPublisher telemetryEventPublisher;

    @Mock
    private ApplianceBehaviorModelRegistry behaviorModelRegistry;

    @Mock
    private ApplianceRuntimeStateStore runtimeStateStore;

    private ApplianceSimulationScheduler scheduler() {
        return new ApplianceSimulationScheduler(taskScheduler, homeRegistry, randomSource, telemetryEventPublisher,
                behaviorModelRegistry, runtimeStateStore, new SimulationProperties(5, 0.0, 1800, 1800));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reRegisteringTheSameApplianceDoesNotScheduleASecondTask() {
        ScheduledFuture<Object> future = mock(ScheduledFuture.class);
        doReturn(future).when(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));

        ApplianceSimulationScheduler scheduler = scheduler();

        UUID applianceId = UUID.randomUUID();
        scheduler.ensureScheduled(applianceId);
        scheduler.ensureScheduled(applianceId);
        scheduler.ensureScheduled(applianceId);

        verify(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));
    }

    @Test
    void usesRegisteredBehaviorModelWhenAvailable() {
        UUID applianceId = UUID.randomUUID();
        UUID homeId = UUID.randomUUID();
        ApplianceConfig config = new ApplianceConfig(applianceId, homeId, "COFFEE_MACHINE", new BigDecimal("1500"),
                new BigDecimal("600"), new BigDecimal("1300"), "COFFEE_MACHINE", "SHORT_HIGH_POWER", null, null);
        when(homeRegistry.find(applianceId)).thenReturn(Optional.of(config));

        ApplianceRuntimeState fixedState = new ApplianceRuntimeState(ApplianceOperatingState.ACTIVE, "NORMAL",
                Instant.now(), null, null, null, null, Instant.now(), null, null, null);
        ApplianceBehaviorModel model = mock(ApplianceBehaviorModel.class);
        when(model.generate(eq(config), any(), any(), any(), eq(randomSource)))
                .thenReturn(new ApplianceBehaviorModel.GeneratedReading(new BigDecimal("12345.00"), fixedState));
        when(behaviorModelRegistry.forConfig(config)).thenReturn(Optional.of(model));
        when(runtimeStateStore.get(applianceId)).thenReturn(Optional.empty());

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doReturn(mock(ScheduledFuture.class)).when(taskScheduler)
                .scheduleWithFixedDelay(runnableCaptor.capture(), any(Duration.class));

        scheduler().ensureScheduled(applianceId);
        runnableCaptor.getValue().run();

        verify(telemetryEventPublisher).publish(homeId, applianceId, new BigDecimal("12345.00"), "ACTIVE", "NORMAL",
                5);
        verify(runtimeStateStore).put(applianceId, fixedState);
    }

    @Test
    void fallsBackToLegacyGeneratorWhenNoBehaviorModelIsRegistered() {
        UUID applianceId = UUID.randomUUID();
        UUID homeId = UUID.randomUUID();
        ApplianceConfig config = new ApplianceConfig(applianceId, homeId, "REFRIGERATOR", new BigDecimal("220"),
                new BigDecimal("80"), new BigDecimal("180"), null, null, null, null);
        when(homeRegistry.find(applianceId)).thenReturn(Optional.of(config));
        when(behaviorModelRegistry.forConfig(config)).thenReturn(Optional.empty());
        when(randomSource.nextDouble()).thenReturn(0.5);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doReturn(mock(ScheduledFuture.class)).when(taskScheduler)
                .scheduleWithFixedDelay(runnableCaptor.capture(), any(Duration.class));

        scheduler().ensureScheduled(applianceId);
        runnableCaptor.getValue().run();

        verify(telemetryEventPublisher).publish(eq(homeId), eq(applianceId), any(BigDecimal.class), eq(null),
                eq(null), eq(5));
        verify(runtimeStateStore, never()).put(any(), any());
        verify(runtimeStateStore, never()).get(any());
    }

    @Test
    void reportsTheConfiguredIntervalOnlyOnTheFirstTickThenFloorsSubsequentGapsAtOneSecond() {
        UUID applianceId = UUID.randomUUID();
        UUID homeId = UUID.randomUUID();
        ApplianceConfig config = new ApplianceConfig(applianceId, homeId, "REFRIGERATOR", new BigDecimal("220"),
                new BigDecimal("80"), new BigDecimal("180"), null, null, null, null);
        when(homeRegistry.find(applianceId)).thenReturn(Optional.of(config));
        when(behaviorModelRegistry.forConfig(config)).thenReturn(Optional.empty());
        when(randomSource.nextDouble()).thenReturn(0.5);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doReturn(mock(ScheduledFuture.class)).when(taskScheduler)
                .scheduleWithFixedDelay(runnableCaptor.capture(), any(Duration.class));

        scheduler().ensureScheduled(applianceId);
        // Two ticks back to back (no real 5s wait) — the second must report the real ~0s gap,
        // floored to 1, not silently re-report the configured 5s interval.
        runnableCaptor.getValue().run();
        runnableCaptor.getValue().run();

        ArgumentCaptor<Integer> elapsedCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(telemetryEventPublisher, times(2)).publish(eq(homeId), eq(applianceId),
                any(BigDecimal.class), eq(null), eq(null), elapsedCaptor.capture());

        assertThat(elapsedCaptor.getAllValues()).containsExactly(5, 1);
    }
}
