package com.vegawatt.sensors.simulation;

import java.util.List;
import java.util.Map;

/**
 * One tuned {@link ApplianceProfile} per known appliance type, matching the presets offered by
 * the registration wizard (see {@code applianceTypes.ts}). Types outside this list (custom
 * appliances) fall back to {@link #GENERIC}: a mild evening-peak / overnight-low shape with no
 * duty cycle or session, since nothing is known about how they're actually used.
 */
final class ApplianceProfiles {

    private ApplianceProfiles() {
    }

    private static final ApplianceProfile REFRIGERATOR = ApplianceProfile.dutyCycled(
            List.of(),
            new ApplianceProfile.DutyCycle(20 * 60, 0.40, 0.20));

    private static final ApplianceProfile AIR_CONDITIONER = ApplianceProfile.dutyCycled(
            List.of(
                    new ApplianceProfile.Bump(15.5, 4.0, 1.6),
                    new ApplianceProfile.Bump(3.0, 4.0, 0.5)),
            new ApplianceProfile.DutyCycle(15 * 60, 0.5, 0.10));

    private static final ApplianceProfile WATER_HEATER = ApplianceProfile.dutyCycled(
            List.of(
                    new ApplianceProfile.Bump(7.0, 1.5, 1.8),
                    new ApplianceProfile.Bump(19.5, 2.0, 1.6),
                    new ApplianceProfile.Bump(2.0, 3.0, 0.3)),
            new ApplianceProfile.DutyCycle(25 * 60, 0.30, 0.05));

    private static final ApplianceProfile WASHING_MACHINE = ApplianceProfile.sessionBased(
            new ApplianceProfile.Session(19.0, 21.0, 1.0, 0.03),
            new ApplianceProfile.Session(10.0, 20.0, 1.0, 0.03));

    private static final ApplianceProfile DISHWASHER = ApplianceProfile.sessionBased(
            new ApplianceProfile.Session(20.0, 22.0, 1.5, 0.03),
            new ApplianceProfile.Session(12.0, 22.0, 1.5, 0.03));

    private static final ApplianceProfile OVEN = ApplianceProfile.sessionBased(
            new ApplianceProfile.Session(18.5, 20.5, 0.75, 0.02),
            new ApplianceProfile.Session(11.5, 20.5, 0.75, 0.02));

    private static final ApplianceProfile TV = ApplianceProfile.bumpOnly(
            List.of(
                    new ApplianceProfile.Bump(20.5, 3.0, 1.8),
                    new ApplianceProfile.Bump(13.0, 2.0, 1.15),
                    new ApplianceProfile.Bump(3.0, 5.0, 0.1)),
            List.of(new ApplianceProfile.Bump(11.5, 4.0, 1.3)));

    private static final ApplianceProfile COMPUTER = ApplianceProfile.bumpOnly(
            List.of(
                    new ApplianceProfile.Bump(20.0, 3.5, 1.5),
                    new ApplianceProfile.Bump(11.0, 3.0, 1.2),
                    new ApplianceProfile.Bump(3.0, 5.0, 0.15)),
            List.of(new ApplianceProfile.Bump(12.0, 4.0, 1.2)));

    static final ApplianceProfile GENERIC = ApplianceProfile.bumpOnly(
            List.of(
                    new ApplianceProfile.Bump(19.0, 2.0, 1.35),
                    new ApplianceProfile.Bump(3.0, 4.0, 0.45)));

    private static final Map<String, ApplianceProfile> BY_TYPE = Map.ofEntries(
            Map.entry("REFRIGERATOR", REFRIGERATOR),
            Map.entry("AIR_CONDITIONER", AIR_CONDITIONER),
            Map.entry("WATER_HEATER", WATER_HEATER),
            Map.entry("WASHING_MACHINE", WASHING_MACHINE),
            Map.entry("DISHWASHER", DISHWASHER),
            Map.entry("OVEN", OVEN),
            Map.entry("TV", TV),
            Map.entry("COMPUTER", COMPUTER));

    static ApplianceProfile forType(String type) {
        return BY_TYPE.getOrDefault(type, GENERIC);
    }
}
