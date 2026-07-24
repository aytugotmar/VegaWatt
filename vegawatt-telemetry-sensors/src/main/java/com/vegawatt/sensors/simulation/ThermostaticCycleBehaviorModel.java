package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.time.Duration;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Thin dispatcher for the {@code THERMOSTATIC_CYCLE} profile — refrigerators/freezers, air
 * conditioners, and electric/fan/water heaters all cycle a compressor or heating element on a
 * thermostat, but the physically-realistic mode names and timings differ enough per device family
 * that a single one-size-fits-all state machine (e.g. a water heater cycling through "DEFROST")
 * reads as nonsense. Delegates to a device-family-specific strategy chosen by {@code config.type()}
 * rather than introducing a separate {@code ApplianceBehaviorProfile} per family, which would need
 * a Postgres migration and keeping two hand-synced enum copies (vegawatt-core +
 * vegawatt-telemetry-sensors) in step for a purely cosmetic/parameter split.
 */
@Component
public class ThermostaticCycleBehaviorModel implements ApplianceBehaviorModel {

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.THERMOSTATIC_CYCLE;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        String type = config.type();
        if ("AIR_CONDITIONER".equals(type)) {
            return AirConditioningThermostatModel.generate(config, previousState, measuredAt, random);
        }
        if ("ELECTRIC_HEATER".equals(type) || "FAN_HEATER".equals(type)) {
            return RoomHeatingThermostatModel.generate(config, previousState, measuredAt, random);
        }
        if ("WATER_HEATER".equals(type)) {
            return WaterHeatingThermostatModel.generate(config, previousState, measuredAt, random);
        }
        // REFRIGERATOR, FREEZER, and any unrecognized type default to the refrigeration model —
        // the two catalog types this profile was originally built for.
        return RefrigerationThermostatModel.generate(config, previousState, measuredAt, random);
    }
}
