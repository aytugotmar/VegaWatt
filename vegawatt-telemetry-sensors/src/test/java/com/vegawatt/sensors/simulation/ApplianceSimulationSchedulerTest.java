package com.vegawatt.sensors.simulation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.vegawatt.sensors.publishing.TelemetryEventPublisher;
import com.vegawatt.sensors.registration.HomeRegistry;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    @SuppressWarnings("unchecked")
    void reRegisteringTheSameApplianceDoesNotScheduleASecondTask() {
        ScheduledFuture<Object> future = mock(ScheduledFuture.class);
        doReturn(future).when(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));

        ApplianceSimulationScheduler scheduler = new ApplianceSimulationScheduler(taskScheduler, homeRegistry,
                randomSource, telemetryEventPublisher, new SimulationProperties(5));

        UUID applianceId = UUID.randomUUID();
        scheduler.ensureScheduled(applianceId);
        scheduler.ensureScheduled(applianceId);
        scheduler.ensureScheduled(applianceId);

        verify(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));
    }
}
