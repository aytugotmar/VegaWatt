package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;

public interface ApplianceBehaviorModel {

    ApplianceBehaviorProfile supportedProfile();

    GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState, ZonedDateTime measuredAt,
                               Duration elapsed, RandomSource random);

    record GeneratedReading(BigDecimal powerWatt, ApplianceRuntimeState nextState) {
    }
}
