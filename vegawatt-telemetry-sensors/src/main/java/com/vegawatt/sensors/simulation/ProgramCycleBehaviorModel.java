package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.time.Duration;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Thin dispatcher for the {@code PROGRAM_CYCLE} profile — washing machines, dishwashers, and
 * dryers all run a multi-stage program, but the physically-realistic stage names, durations, and
 * power shapes differ enough per device (a dishwasher has no "spin" stage; a dryer has no "wash"
 * stage at all) that one shared state machine reads as nonsense for two of the three device
 * types. Delegates to a device-specific strategy chosen by {@code config.type()} rather than
 * introducing a separate {@code ApplianceBehaviorProfile} per device — same rationale as
 * {@link ThermostaticCycleBehaviorModel}'s dispatcher: no Postgres migration, no keeping two
 * hand-synced enum copies in step, for what is otherwise a parameter/naming split.
 */
@Component
public class ProgramCycleBehaviorModel implements ApplianceBehaviorModel {

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.PROGRAM_CYCLE;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        String type = config.type();
        if ("DISHWASHER".equals(type)) {
            return DishwasherProgramModel.generate(config, previousState, measuredAt, random);
        }
        if ("DRYER".equals(type)) {
            return DryerProgramModel.generate(config, previousState, measuredAt, random);
        }
        // WASHING_MACHINE and any unrecognized type default to the washing-machine model — the
        // profile's original device type.
        return WashingMachineProgramModel.generate(config, previousState, measuredAt, random);
    }
}
