package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.publishing.TelemetryEventPublisher;
import com.vegawatt.sensors.registration.ApplianceConfig;
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
    private final ApplianceBehaviorModelRegistry behaviorModelRegistry;
    private final ApplianceRuntimeStateStore runtimeStateStore;
    private final Duration interval;

    public ApplianceSimulationScheduler(TaskScheduler taskScheduler, HomeRegistry homeRegistry,
                                         RandomSource randomSource, TelemetryEventPublisher telemetryEventPublisher,
                                         ApplianceBehaviorModelRegistry behaviorModelRegistry,
                                         ApplianceRuntimeStateStore runtimeStateStore,
                                         SimulationProperties properties) {
        this.taskScheduler = taskScheduler;
        this.homeRegistry = homeRegistry;
        this.randomSource = randomSource;
        this.telemetryEventPublisher = telemetryEventPublisher;
        this.behaviorModelRegistry = behaviorModelRegistry;
        this.runtimeStateStore = runtimeStateStore;
        this.interval = Duration.ofSeconds(properties.telemetryIntervalSeconds());
    }

    public void ensureScheduled(UUID applianceId) {
        scheduledTasks.computeIfAbsent(applianceId,
                id -> taskScheduler.scheduleWithFixedDelay(() -> tick(id), interval));
    }

    private void tick(UUID applianceId) {
        homeRegistry.find(applianceId).ifPresent(config -> {
            ZonedDateTime now = ZonedDateTime.now(SIMULATION_ZONE);
            GeneratedTelemetry telemetry = generateTelemetry(applianceId, config, now);
            String operatingState = telemetry.operatingState() == null ? null : telemetry.operatingState().name();
            telemetryEventPublisher.publish(config.homeId(), applianceId, telemetry.powerWatt(), operatingState,
                    telemetry.operatingMode(), (int) interval.toSeconds());
        });
    }

    /** Dispatches to a stateful {@link ApplianceBehaviorModel} when one is registered for the
     * appliance's behavior profile; otherwise falls back to the legacy stateless generator
     * unchanged (covers custom appliances and any catalog profile without a model yet) — those
     * appliances have no runtime state, so operatingState/operatingMode stay null rather than a
     * fabricated value. */
    private GeneratedTelemetry generateTelemetry(UUID applianceId, ApplianceConfig config, ZonedDateTime now) {
        return behaviorModelRegistry.forConfig(config)
                .map(model -> {
                    ApplianceRuntimeState previousState = runtimeStateStore.get(applianceId).orElse(null);
                    Duration elapsed = previousState == null ? interval
                            : Duration.between(previousState.previousMeasurementAt(), now.toInstant());
                    ApplianceBehaviorModel.GeneratedReading reading = model.generate(config, previousState, now,
                            elapsed, randomSource);
                    runtimeStateStore.put(applianceId, reading.nextState());
                    return new GeneratedTelemetry(reading.powerWatt(), reading.nextState().operatingState(),
                            reading.nextState().operatingMode());
                })
                .orElseGet(() -> new GeneratedTelemetry(TelemetryGenerator.generatePowerWatt(config, randomSource, now),
                        null, null));
    }

    private record GeneratedTelemetry(BigDecimal powerWatt, ApplianceOperatingState operatingState,
                                       String operatingMode) {
    }
}
