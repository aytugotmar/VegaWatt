package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Drives a multi-day simulated run (17280 ticks/day at the default 5s interval, across a week) for
 * a representative appliance per newly-touched/added behavior model, using a seeded (not
 * fixed-sequence) random source so the run is reproducible but genuinely varied tick to tick —
 * closer to how the real simulator behaves than the single/few-tick unit tests above. Asserts the
 * aggregate invariants from the finding: OFF never exceeds the standby ceiling, ACTIVE never
 * contradicts an "OFF" mode, and daily session/program/charge counts never exceed each device's
 * configured cap. A full week (rather than a single day) is simulated so that scenarios whose
 * daily session count can legitimately be zero (§5.1 scheduling allows some devices, e.g. an oven,
 * to have no planned session on a given day) still reliably show at least one active session
 * somewhere in the run, and so the §5.1 fix — session starts spread across the day instead of
 * clumping at window-open — can be verified directly.
 */
class FullDaySimulationTest {

    private static final int TICKS_PER_DAY = 17280; // 24h at 5s intervals
    private static final int DAYS_TO_SIMULATE = 7;
    // Several seeds, not just one: a single fixed sequence could hide a bug that only shows up
    // for certain random draws (e.g. a boundary condition in DiurnalCurve's scheduling). Each
    // scenario must hold its invariants across all of them, not just this one lucky draw.
    private static final long[] SEEDS = {42L, 7L, 123456789L};

    private record Scenario(String label, ApplianceBehaviorModel model, ApplianceConfig config, Integer dailyCap,
                             boolean expectSpreadStartTimes) {
    }

    private static List<Scenario> scenarios() {
        return List.of(
                new Scenario("kettle", new ShortHighPowerBehaviorModel(),
                        configFor("kettle", "KETTLE", "KETTLE", "SHORT_HIGH_POWER", "2200", "1800", "2100", "1"), 8,
                        true),
                new Scenario("washing machine", new ProgramCycleBehaviorModel(),
                        configFor("washing-machine", "WASHING_MACHINE", "WASHING_MACHINE", "PROGRAM_CYCLE", "2300",
                                "300", "2200", "3"), 2, true),
                new Scenario("dishwasher", new ProgramCycleBehaviorModel(),
                        configFor("dishwasher", "DISHWASHER", "DISHWASHER", "PROGRAM_CYCLE", "2300", "300", "2200",
                                "3"), 2, true),
                new Scenario("dryer", new ProgramCycleBehaviorModel(),
                        configFor("dryer", "DRYER", "DRYER", "PROGRAM_CYCLE", "2300", "300", "2200", "3"), 2, true),
                new Scenario("oven", new ThermostaticSessionBehaviorModel(),
                        configFor("oven", "OVEN", "OVEN", "THERMOSTATIC_SESSION", "2600", "500", "2500", "3"), 3,
                        true),
                new Scenario("iron", new ThermostaticSessionBehaviorModel(),
                        configFor("iron", "IRON", "IRON", "THERMOSTATIC_SESSION", "1800", "50", "1600", "3"), 2, true),
                new Scenario("desk lamp", new ManualSwitchBehaviorModel(),
                        configFor("desk-lamp", "DESK_LAMP", "DESK_LAMP", "MANUAL_SWITCH", "40", "5", "15", "0"), null,
                        false),
                new Scenario("laptop charger", new ChargingCurveBehaviorModel(),
                        configFor("laptop-charger", "LAPTOP", "LAPTOP", "CHARGING_CURVE", "100", "5", "90", "1"), 2,
                        true),
                new Scenario("robot vacuum", new ChargingAndSessionBehaviorModel(),
                        configFor("robot-vacuum", "ROBOT_VACUUM", "ROBOT_VACUUM", "CHARGING_AND_SESSION", "60", "20",
                                "50", "1"), 2, true),
                new Scenario("air purifier", new AlwaysOnVariableBehaviorModel(),
                        configFor("air-purifier", "AIR_PURIFIER", "AIR_PURIFIER", "ALWAYS_ON_VARIABLE", "50", "10",
                                "40", "0"), null, false)
        );
    }

    /**
     * Fixed, deterministic per-scenario UUIDs (derived from the scenario label) instead of
     * {@code UUID.randomUUID()} — several behavior models hash {@code applianceId()} into
     * {@code DiurnalCurve}'s per-device jitter (planned session start hour, demand curve phase,
     * smoothed noise), so a random UUID made any jitter-dependent assertion failure irreproducible
     * between runs even with the seeded {@link Random} held fixed.
     */
    private static ApplianceConfig configFor(String label, String type, String catalogCode, String behaviorProfile,
                                              String safeLimit, String minWatt, String maxWatt, String standbyMax) {
        UUID homeId = UUID.nameUUIDFromBytes(("home-" + label).getBytes(StandardCharsets.UTF_8));
        UUID applianceId = UUID.nameUUIDFromBytes(("appliance-" + label).getBytes(StandardCharsets.UTF_8));
        return new ApplianceConfig(homeId, applianceId, type, new BigDecimal(safeLimit), new BigDecimal(minWatt),
                new BigDecimal(maxWatt), catalogCode, behaviorProfile, null, new BigDecimal(standbyMax));
    }

    @Test
    void everyScenarioRespectsItsInvariantsAcrossAWeek() {
        for (Scenario scenario : scenarios()) {
            for (long seed : SEEDS) {
                runAndVerify(scenario, seed);
            }
        }
    }

    private void runAndVerify(Scenario scenario, long seed) {
        Random seededRandom = new Random(seed);
        RandomSource randomSource = seededRandom::nextDouble;
        ZonedDateTime midnight = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        BigDecimal standbyMax = scenario.config().standbyMaxWatt() != null ? scenario.config().standbyMaxWatt()
                : BigDecimal.ZERO;

        ApplianceRuntimeState state = null;
        int sessionsStarted = 0;
        boolean everActive = false;
        int maxSessionsSeenPerDay = 0;
        Set<Integer> sessionStartHours = new HashSet<>();

        int totalTicks = TICKS_PER_DAY * DAYS_TO_SIMULATE;
        for (int tick = 0; tick < totalTicks; tick++) {
            ZonedDateTime measuredAt = midnight.plusSeconds(5L * tick);
            var reading = scenario.model().generate(scenario.config(), state, measuredAt, Duration.ofSeconds(5),
                    randomSource);
            ApplianceRuntimeState next = reading.nextState();

            boolean wasActive = state != null && state.operatingState() == ApplianceOperatingState.ACTIVE;
            boolean nowActive = next.operatingState() == ApplianceOperatingState.ACTIVE;
            if (nowActive && !wasActive) {
                sessionsStarted++;
                sessionStartHours.add(measuredAt.getHour());
            }
            if (nowActive) {
                everActive = true;
                assertThat(next.operatingMode())
                        .as("%s (seed %d): ACTIVE state must never contradict an OFF mode (tick %d)", scenario.label(), seed, tick)
                        .isNotEqualTo("OFF");
            }
            if (!nowActive) {
                assertThat(reading.powerWatt())
                        .as("%s (seed %d): non-active (OFF/STANDBY) power must never exceed the standby ceiling (tick %d)",
                                scenario.label(), seed, tick)
                        .isLessThanOrEqualTo(standbyMax);
            }

            maxSessionsSeenPerDay = Math.max(maxSessionsSeenPerDay, next.sessionsTodayAt(measuredAt.toLocalDate()));
            state = next;
        }

        assertThat(everActive).as("%s (seed %d): must be active at least once across a full week", scenario.label(), seed)
                .isTrue();
        if (scenario.dailyCap() != null) {
            assertThat(maxSessionsSeenPerDay)
                    .as("%s (seed %d): daily session count must never exceed its configured cap", scenario.label(), seed)
                    .isLessThanOrEqualTo(scenario.dailyCap());
            assertThat(sessionsStarted)
                    .as("%s (seed %d): number of session starts must never exceed cap x days", scenario.label(), seed)
                    .isLessThanOrEqualTo(scenario.dailyCap() * DAYS_TO_SIMULATE);
        }
        if (scenario.expectSpreadStartTimes()) {
            assertThat(sessionStartHours)
                    .as("%s (seed %d): session starts must be spread across different hours, not clumped at one hour",
                            scenario.label(), seed)
                    .hasSizeGreaterThan(1);
        }
    }
}
