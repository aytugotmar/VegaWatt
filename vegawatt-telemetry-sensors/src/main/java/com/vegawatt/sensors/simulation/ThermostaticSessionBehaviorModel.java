package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.time.Duration;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Thin dispatcher for the {@code THERMOSTATIC_SESSION} profile — ovens and irons both run a
 * one-off heat-up-then-use session, but their realistic mode names and durations are very
 * different (an oven bakes for tens of minutes and holds warm afterward; an iron heats in
 * seconds and is actively used for a much shorter session with no "keep warm" phase at all).
 * Delegates to a device-specific strategy chosen by {@code config.type()} — same rationale as
 * {@link ThermostaticCycleBehaviorModel}'s dispatcher.
 */
@Component
public class ThermostaticSessionBehaviorModel implements ApplianceBehaviorModel {

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.THERMOSTATIC_SESSION;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        if ("IRON".equals(config.type())) {
            return IronSessionBehaviorModel.generate(config, previousState, measuredAt, random);
        }
        // OVEN and any unrecognized type default to the oven model — the profile's original
        // device type.
        return OvenSessionBehaviorModel.generate(config, previousState, measuredAt, random);
    }
}
