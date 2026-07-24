package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Drives a full simulated day (17280 ticks at the default 5s interval) for a representative
 * appliance per newly-touched/added behavior model, using a seeded (not fixed-sequence) random
 * source so the run is reproducible but genuinely varied tick to tick — closer to how the real
 * simulator behaves than the single/few-tick unit tests above. Asserts the aggregate invariants
 * from the finding: OFF never exceeds the standby ceiling, ACTIVE never contradicts an "OFF" mode,
 * and daily session/program/charge counts never exceed each device's configured cap.
 */
class FullDaySimulationTest {

    private static final int TICKS_PER_DAY = 17280; // 24h at 5s intervals
    private static final long SEED = 42L;

    private record Scenario(String label, ApplianceBehaviorModel model, ApplianceConfig config, Integer dailyCap) {
    }

    private static List<Scenario> scenarios() {
        return List.of(
                new Scenario("kettle", new ShortHighPowerBehaviorModel(),
                        configFor("KETTLE", "KETTLE", "SHORT_HIGH_POWER", "2200", "1800", "2100", "1"), 8),
                new Scenario("washing machine", new ProgramCycleBehaviorModel(),
                        configFor("WASHING_MACHINE", "WASHING_MACHINE", "PROGRAM_CYCLE", "2300", "300", "2200", "3"), 2),
                new Scenario("oven", new ThermostaticSessionBehaviorModel(),
                        configFor("OVEN", "OVEN", "THERMOSTATIC_SESSION", "2600", "500", "2500", "3"), 3),
                new Scenario("desk lamp", new ManualSwitchBehaviorModel(),
                        configFor("DESK_LAMP", "DESK_LAMP", "MANUAL_SWITCH", "40", "5", "15", "0"), null),
                new Scenario("laptop charger", new ChargingCurveBehaviorModel(),
                        configFor("LAPTOP", "LAPTOP", "CHARGING_CURVE", "100", "5", "90", "1"), 2),
                new Scenario("robot vacuum", new ChargingAndSessionBehaviorModel(),
                        configFor("ROBOT_VACUUM", "ROBOT_VACUUM", "CHARGING_AND_SESSION", "60", "20", "50", "1"), 2),
                new Scenario("air purifier", new AlwaysOnVariableBehaviorModel(),
                        configFor("AIR_PURIFIER", "AIR_PURIFIER", "ALWAYS_ON_VARIABLE", "50", "10", "40", "0"), null)
        );
    }

    private static ApplianceConfig configFor(String type, String catalogCode, String behaviorProfile,
                                              String safeLimit, String minWatt, String maxWatt, String standbyMax) {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), type, new BigDecimal(safeLimit),
                new BigDecimal(minWatt), new BigDecimal(maxWatt), catalogCode, behaviorProfile, null,
                new BigDecimal(standbyMax));
    }

    @Test
    void everyScenarioRespectsItsInvariantsAcrossAFullSimulatedDay() {
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
        int maxSessionsSeenToday = 0;

        for (int tick = 0; tick < TICKS_PER_DAY; tick++) {
            ZonedDateTime measuredAt = midnight.plusSeconds(5L * tick);
            var reading = scenario.model().generate(scenario.config(), state, measuredAt, Duration.ofSeconds(5),
                    randomSource);
            ApplianceRuntimeState next = reading.nextState();

            boolean wasActive = state != null && state.operatingState() == ApplianceOperatingState.ACTIVE;
            boolean nowActive = next.operatingState() == ApplianceOperatingState.ACTIVE;
            if (nowActive && !wasActive) {
                sessionsStarted++;
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

            maxSessionsSeenToday = Math.max(maxSessionsSeenToday, next.sessionsTodayAt(measuredAt.toLocalDate()));
            state = next;
        }

        assertThat(everActive).as("%s: must be active at least once across a full day", scenario.label()).isTrue();
        if (scenario.dailyCap() != null) {
            assertThat(maxSessionsSeenToday)
                    .as("%s: daily session count must never exceed its configured cap", scenario.label())
                    .isLessThanOrEqualTo(scenario.dailyCap());
            assertThat(sessionsStarted)
                    .as("%s: number of session starts must never exceed its configured daily cap", scenario.label())
                    .isLessThanOrEqualTo(scenario.dailyCap());
        }
    }
}
