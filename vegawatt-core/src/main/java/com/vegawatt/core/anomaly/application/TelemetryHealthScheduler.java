package com.vegawatt.core.anomaly.application;

import com.vegawatt.core.common.ApplianceHealthStatus;
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
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Detects TELEMETRY_STALE/TELEMETRY_OFFLINE by periodically scanning every appliance's live state
 * for silence (unlike every other trigger in this package, which reacts to an incoming telemetry
 * event, this one has to notice the ABSENCE of one). TELEMETRY_RESUMED is the mirror image and is
 * detected reactively instead, in {@code ProcessTelemetryUseCase}, when a real event arrives after
 * a period of staleness/offline.
 *
 * <p>Applies uniformly to every appliance regardless of behavior profile — mirroring how
 * SAFE_POWER_LIMIT_BREACHED already applies universally while {@code SupportedTriggerCatalog} is
 * the one place that decides which profiles advertise it. No distributed locking: single-instance
 * deployment is assumed, consistent with {@code SnapshotScheduler}/{@code OutboxRelayScheduler}/
 * {@code NotificationJobWorker}, none of which coordinate across multiple app instances either.
 */
@Component
public class TelemetryHealthScheduler {

    private static final Logger log = LoggerFactory.getLogger(TelemetryHealthScheduler.class);

    private final ApplianceLiveStatePort applianceLiveStatePort;
    private final OperationalEventRepository operationalEventRepository;
    private final NotificationJobRepository notificationJobRepository;
    private final ClockProvider clockProvider;
    private final TelemetryHealthProperties properties;

    public TelemetryHealthScheduler(ApplianceLiveStatePort applianceLiveStatePort,
                                     OperationalEventRepository operationalEventRepository,
                                     NotificationJobRepository notificationJobRepository,
                                     ClockProvider clockProvider, TelemetryHealthProperties properties) {
        this.applianceLiveStatePort = applianceLiveStatePort;
        this.operationalEventRepository = operationalEventRepository;
        this.notificationJobRepository = notificationJobRepository;
        this.clockProvider = clockProvider;
        this.properties = properties;
    }

    @Scheduled(fixedRateString = "${vegawatt.telemetry-health.sweep-interval-seconds}", timeUnit = TimeUnit.SECONDS)
    public void sweep() {
        Instant now = clockProvider.now();
        for (ApplianceLiveState state : applianceLiveStatePort.getAll()) {
            // Cheap in-memory pre-check against the scan snapshot so the common case (a healthy
            // appliance) never pays for a transactional Ignite update.
            if (nextStatus(state.telemetryHealthStatus(), state.lastUpdatedAt(), now) == null) {
                continue;
            }
            try {
                evaluateAndRecord(state.homeId(), state.applianceId(), now);
            } catch (RuntimeException e) {
                log.error("Failed to evaluate telemetry health for appliance {}", state.applianceId(), e);
            }
        }
    }

    private void evaluateAndRecord(UUID homeId, UUID applianceId, Instant now) {
        AtomicReference<ApplianceHealthStatus> transitionedTo = new AtomicReference<>();

        applianceLiveStatePort.update(homeId, applianceId, existing -> {
            if (existing == null) {
                return null;
            }
            // Re-checked against the freshly re-read state (not the scan snapshot) so an appliance
            // that received real telemetry between the scan and this update is never clobbered back
            // into STALE/OFFLINE.
            ApplianceHealthStatus next = nextStatus(existing.telemetryHealthStatus(), existing.lastUpdatedAt(), now);
            if (next == null) {
                return existing;
            }
            transitionedTo.set(next);
            return withHealthStatus(existing, next);
        });

        ApplianceHealthStatus next = transitionedTo.get();
        if (next != null) {
            recordTransition(homeId, applianceId, next, now);
        }
    }

    private ApplianceHealthStatus nextStatus(ApplianceHealthStatus current, Instant lastUpdatedAt, Instant now) {
        long elapsedSeconds = Duration.between(lastUpdatedAt, now).getSeconds();
        if (elapsedSeconds >= properties.offlineAfterSeconds() && current != ApplianceHealthStatus.OFFLINE) {
            return ApplianceHealthStatus.OFFLINE;
        }
        if (elapsedSeconds >= properties.staleAfterSeconds() && current == ApplianceHealthStatus.NORMAL) {
            return ApplianceHealthStatus.STALE;
        }
        return null;
    }

    private void recordTransition(UUID homeId, UUID applianceId, ApplianceHealthStatus next, Instant now) {
        boolean offline = next == ApplianceHealthStatus.OFFLINE;
        OperationalEventType eventType = offline ? OperationalEventType.APPLIANCE_TELEMETRY_OFFLINE
                : OperationalEventType.APPLIANCE_TELEMETRY_STALE;
        AdvisoryTriggerType triggerType = offline ? AdvisoryTriggerType.TELEMETRY_OFFLINE
                : AdvisoryTriggerType.TELEMETRY_STALE;
        String details = "telemetry health transitioned to %s, detectedAt=%s".formatted(next, now);

        OperationalEvent saved = operationalEventRepository.save(
                OperationalEvent.create(homeId, applianceId, eventType, now, details));
        notificationJobRepository.save(NotificationJob.create(saved.id(), homeId, triggerType, now));
    }

    private static ApplianceLiveState withHealthStatus(ApplianceLiveState state, ApplianceHealthStatus status) {
        return new ApplianceLiveState(state.homeId(), state.applianceId(), state.applianceName(),
                state.applianceType(), state.safePowerLimitWatt(), state.currentPowerWatt(), state.operatingState(),
                state.operatingMode(), state.accumulatedEnergyKwh(), state.consecutiveBreachCount(),
                state.anomalous(), state.standbyBreachCount(), state.standbyRecoveryCount(),
                state.standbyAnomalyActive(), status, state.lastUpdatedAt());
    }
}
