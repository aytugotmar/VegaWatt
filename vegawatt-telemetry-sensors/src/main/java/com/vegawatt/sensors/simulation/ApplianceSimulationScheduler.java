package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.publishing.TelemetryEventPublisher;
import com.vegawatt.sensors.registration.HomeRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

/**
 * Runs exactly one recurring simulation tick per appliance. The tick always reads the
 * appliance's current configuration from {@link HomeRegistry}, so a re-registration that
 * updates simulation ranges takes effect on the next tick without ever needing to cancel or
 * reschedule a task.
 */
@Component
public class ApplianceSimulationScheduler {

    private static final ZoneId SIMULATION_ZONE = ZoneId.of("Europe/Istanbul");

    private final Map<UUID, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private final TaskScheduler taskScheduler;
    private final HomeRegistry homeRegistry;
    private final RandomSource randomSource;
    private final TelemetryEventPublisher telemetryEventPublisher;
    private final Duration interval;

    public ApplianceSimulationScheduler(TaskScheduler taskScheduler, HomeRegistry homeRegistry,
                                         RandomSource randomSource, TelemetryEventPublisher telemetryEventPublisher,
                                         SimulationProperties properties) {
        this.taskScheduler = taskScheduler;
        this.homeRegistry = homeRegistry;
        this.randomSource = randomSource;
        this.telemetryEventPublisher = telemetryEventPublisher;
        this.interval = Duration.ofSeconds(properties.telemetryIntervalSeconds());
    }

    public void ensureScheduled(UUID applianceId) {
        scheduledTasks.computeIfAbsent(applianceId,
                id -> taskScheduler.scheduleWithFixedDelay(() -> tick(id), interval));
    }

    private void tick(UUID applianceId) {
        homeRegistry.find(applianceId).ifPresent(config -> {
            ZonedDateTime now = ZonedDateTime.now(SIMULATION_ZONE);
            BigDecimal powerWatt = TelemetryGenerator.generatePowerWatt(config, randomSource, now);
            telemetryEventPublisher.publish(config.homeId(), applianceId, powerWatt,
                    (int) interval.toSeconds());
        });
    }
}
