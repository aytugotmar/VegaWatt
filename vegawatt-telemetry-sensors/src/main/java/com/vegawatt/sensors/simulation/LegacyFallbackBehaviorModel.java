package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Safety-net model for any behavior profile without a dedicated implementation (today: only the
 * unused {@code FLOW_TRIGGERED} enum value, plus any unparseable/null profile string) — every
 * currently-seeded catalog profile now has a real model. Derives {@code operatingState}/
 * {@code operatingMode} from the computed {@code powerWatt} relative to the appliance's standby
 * ceiling, rather than freezing at whatever {@link ApplianceRuntimeState#initial} set on the first
 * tick: the old version passed the previous state through unchanged forever, so a fallback-modeled
 * appliance would report OFF/STANDBY permanently even while genuinely drawing real, fluctuating
 * power.
 */
@Component
public class LegacyFallbackBehaviorModel implements ApplianceBehaviorModel {

    private static final Logger log = LoggerFactory.getLogger(LegacyFallbackBehaviorModel.class);
    private final Set<UUID> loggedAppliances = ConcurrentHashMap.newKeySet();

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return null;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        if (loggedAppliances.add(config.applianceId())) {
            log.info("Using legacy behavior fallback: applianceId={}, behaviorProfile={}",
                    config.applianceId(), config.behaviorProfile());
        }
        BigDecimal powerWatt = TelemetryGenerator.generatePowerWatt(config, random, measuredAt);

        BigDecimal standbyMax = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : BigDecimal.ZERO;
        ApplianceOperatingState operatingState;
        String operatingMode;
        if (powerWatt.compareTo(standbyMax) > 0) {
            operatingState = ApplianceOperatingState.ACTIVE;
            operatingMode = "RUNNING";
        } else if (powerWatt.signum() > 0) {
            operatingState = ApplianceOperatingState.STANDBY;
            operatingMode = "STANDBY";
        } else {
            operatingState = ApplianceOperatingState.OFF;
            operatingMode = "OFF";
        }

        Instant now = measuredAt.toInstant();
        Instant stateStartedAt = (previousState != null && previousState.operatingState() == operatingState)
                ? previousState.stateStartedAt() : now;

        ApplianceRuntimeState nextState = new ApplianceRuntimeState(operatingState, operatingMode, stateStartedAt,
                null, null, null, null, now, null, null, null, 0, null);
        return new GeneratedReading(powerWatt, nextState);
    }
}
