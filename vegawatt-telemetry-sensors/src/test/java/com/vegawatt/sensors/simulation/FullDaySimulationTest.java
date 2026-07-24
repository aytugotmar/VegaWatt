package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
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
    private static final long SEED = 42L;

    private record Scenario(String label, ApplianceBehaviorModel model, ApplianceConfig config, Integer dailyCap,
                             boolean expectSpreadStartTimes) {
    }

    private static List<Scenario> scenarios() {
        return List.of(
                new Scenario("kettle", new ShortHighPowerBehaviorModel(),
                        configFor("KETTLE", "KETTLE", "SHORT_HIGH_POWER", "2200", "1800", "2100", "1"), 8, true),
                new Scenario("washing machine", new ProgramCycleBehaviorModel(),
                        configFor("WASHING_MACHINE", "WASHING_MACHINE", "PROGRAM_CYCLE", "2300", "300", "2200", "3"),
                        2, true),
                new Scenario("dishwasher", new ProgramCycleBehaviorModel(),
                        configFor("DISHWASHER", "DISHWASHER", "PROGRAM_CYCLE", "2300", "300", "2200", "3"), 2, true),
                new Scenario("dryer", new ProgramCycleBehaviorModel(),
                        configFor("DRYER", "DRYER", "PROGRAM_CYCLE", "2300", "300", "2200", "3"), 2, true),
                new Scenario("oven", new ThermostaticSessionBehaviorModel(),
                        configFor("OVEN", "OVEN", "THERMOSTATIC_SESSION", "2600", "500", "2500", "3"), 3, true),
                new Scenario("iron", new ThermostaticSessionBehaviorModel(),
                        configFor("IRON", "IRON", "THERMOSTATIC_SESSION", "1800", "50", "1600", "3"), 2, true),
                new Scenario("desk lamp", new ManualSwitchBehaviorModel(),
                        configFor("DESK_LAMP", "DESK_LAMP", "MANUAL_SWITCH", "40", "5", "15", "0"), null, false),
                new Scenario("laptop charger", new ChargingCurveBehaviorModel(),
                        configFor("LAPTOP", "LAPTOP", "CHARGING_CURVE", "100", "5", "90", "1"), 2, true),
                new Scenario("robot vacuum", new ChargingAndSessionBehaviorModel(),
                        configFor("ROBOT_VACUUM", "ROBOT_VACUUM", "CHARGING_AND_SESSION", "60", "20", "50", "1"), 2,
                        true),
                new Scenario("air purifier", new AlwaysOnVariableBehaviorModel(),
                        configFor("AIR_PURIFIER", "AIR_PURIFIER", "ALWAYS_ON_VARIABLE", "50", "10", "40", "0"), null,
                        false)
        );
    }

    private static ApplianceConfig configFor(String type, String catalogCode, String behaviorProfile,
                                              String safeLimit, String minWatt, String maxWatt, String standbyMax) {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), type, new BigDecimal(safeLimit),
                new BigDecimal(minWatt), new BigDecimal(maxWatt), catalogCode, behaviorProfile, null,
                new BigDecimal(standbyMax));
    }

    @Test
    void everyScenarioRespectsItsInvariantsAcrossAWeek() {
        for (Scenario scenario : scenarios()) {
            runAndVerify(scenario);
        }
    }

    private void runAndVerify(Scenario scenario) {
        Random seededRandom = new Random(SEED);
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
                        .as("%s: ACTIVE state must never contradict an OFF mode (tick %d)", scenario.label(), tick)
                        .isNotEqualTo("OFF");
            }
            if (!nowActive) {
                assertThat(reading.powerWatt())
                        .as("%s: non-active (OFF/STANDBY) power must never exceed the standby ceiling (tick %d)",
                                scenario.label(), tick)
                        .isLessThanOrEqualTo(standbyMax);
            }

            maxSessionsSeenPerDay = Math.max(maxSessionsSeenPerDay, next.sessionsTodayAt(measuredAt.toLocalDate()));
            state = next;
        }

        assertThat(everActive).as("%s: must be active at least once across a full week", scenario.label()).isTrue();
        if (scenario.dailyCap() != null) {
            assertThat(maxSessionsSeenPerDay)
                    .as("%s: daily session count must never exceed its configured cap", scenario.label())
                    .isLessThanOrEqualTo(scenario.dailyCap());
            assertThat(sessionsStarted)
                    .as("%s: number of session starts must never exceed cap x days", scenario.label())
                    .isLessThanOrEqualTo(scenario.dailyCap() * DAYS_TO_SIMULATE);
        }
        if (scenario.expectSpreadStartTimes()) {
            assertThat(sessionStartHours)
                    .as("%s: session starts must be spread across different hours, not clumped at one hour",
                            scenario.label())
                    .hasSizeGreaterThan(1);
        }
    }
}
