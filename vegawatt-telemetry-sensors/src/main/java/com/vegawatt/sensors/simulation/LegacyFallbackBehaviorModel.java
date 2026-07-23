package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
        ApplianceRuntimeState nextState = previousState != null ? previousState : ApplianceRuntimeState.initial(measuredAt.toInstant());
        return new GeneratedReading(powerWatt, nextState);
    }
}
