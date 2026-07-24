package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Covers all 3 device-family dispatch targets — a washing machine keeps its WASHING/RINSING/
 * SPINNING stages, while a dishwasher and dryer get their own device-appropriate stage names
 * instead of the one-size-fits-all cycle (a dishwasher never "spins"; a dryer never "washes"). */
class ProgramCycleBehaviorModelTest {

    private static final ZonedDateTime MORNING = ZonedDateTime.now().withHour(9).withMinute(0).withSecond(0);
    private static final ProgramCycleBehaviorModel MODEL = new ProgramCycleBehaviorModel();

    private static ApplianceConfig washingMachineConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "WASHING_MACHINE", new BigDecimal("2300"),
                new BigDecimal("300"), new BigDecimal("2200"), "WASHING_MACHINE", "PROGRAM_CYCLE", null,
                new BigDecimal("3"));
    }

    private static ApplianceConfig dishwasherConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "DISHWASHER", new BigDecimal("2300"),
                new BigDecimal("300"), new BigDecimal("2200"), "DISHWASHER", "PROGRAM_CYCLE", null,
                new BigDecimal("3"));
    }

    private static ApplianceConfig dryerConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "DRYER", new BigDecimal("2300"),
                new BigDecimal("300"), new BigDecimal("2200"), "DRYER", "PROGRAM_CYCLE", null, new BigDecimal("3"));
    }

    @Test
    void spinStageEndingNeverEmitsActiveStateWithOffMode() {
        ApplianceConfig config = washingMachineConfig();

        // Force straight into SPINNING already 751s in (past its 750s threshold), so the very next
        // tick transitions to OFF.
        ApplianceRuntimeState spinning = new ApplianceRuntimeState(ApplianceOperatingState.ACTIVE, "SPINNING",
                MORNING.toInstant().minusSeconds(751), null, null, null, null, MORNING.toInstant(), null, null, null,
                0, MORNING.toLocalDate());

        var reading = MODEL.generate(config, spinning, MORNING.plusSeconds(5), Duration.ofSeconds(5), () -> 0.5);

        assertThat(reading.nextState().operatingMode()).isEqualTo("OFF");
        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.OFF);
        assertThat(reading.powerWatt()).isLessThanOrEqualTo(config.standbyMaxWatt());
    }

    @Test
    void neverStartsMoreProgramsThanTheDailyCap() {
        ApplianceConfig config = washingMachineConfig();
        ApplianceRuntimeState state = null;
        int programsStarted = 0;

        // Winning roll (0.0) every tick would start a new program immediately after each one ends.
        // Kept within the same simulated calendar day (9am + ~11h &lt; midnight) so the daily cap
        // isn't legitimately reset mid-test.
        for (int tick = 0; tick < 8000; tick++) {
            ZonedDateTime measuredAt = MORNING.plusSeconds(5L * tick);
            var reading = MODEL.generate(config, state, measuredAt, Duration.ofSeconds(5), () -> 0.0);
            boolean programJustStarted = "OFF".equals(state == null ? "OFF" : state.operatingMode())
                    && "WASHING".equals(reading.nextState().operatingMode());
            if (programJustStarted) {
                programsStarted++;
            }
            state = reading.nextState();
        }

        assertThat(programsStarted).isLessThanOrEqualTo(2);
    }

    @Test
    void dishwasherNeverEmitsWashingMachineOnlyStages() {
        ApplianceConfig config = dishwasherConfig();

        ApplianceRuntimeState state = new ApplianceRuntimeState(ApplianceOperatingState.ACTIVE, "WASHING",
                MORNING.toInstant(), null, null, null, null, MORNING.toInstant(), null, null, null, 0,
                MORNING.toLocalDate());

        // Walk the dishwasher's own stage machine forward far enough to pass through every stage;
        // it should never emit SPINNING or RINSING-then-SPINNING — the washing-machine-only stage.
        for (int tick = 0; tick < 1200; tick++) { // 6000s, longer than the whole ~105 min cycle
            var reading = MODEL.generate(config, state, MORNING.plusSeconds(5L * (tick + 1)), Duration.ofSeconds(5),
                    () -> 0.5);
            assertThat(reading.nextState().operatingMode()).isNotEqualTo("SPINNING");
            state = reading.nextState();
        }
    }

    @Test
    void dishwasherDryingStageEndingNeverEmitsActiveStateWithOffMode() {
        ApplianceConfig config = dishwasherConfig();

        // Force straight into DRYING already past its threshold, so the very next tick transitions
        // to OFF.
        ApplianceRuntimeState drying = new ApplianceRuntimeState(ApplianceOperatingState.ACTIVE, "DRYING",
                MORNING.toInstant().minusSeconds(1801), null, null, null, null, MORNING.toInstant(), null, null, null,
                0, MORNING.toLocalDate());

        var reading = MODEL.generate(config, drying, MORNING.plusSeconds(5), Duration.ofSeconds(5), () -> 0.5);

        assertThat(reading.nextState().operatingMode()).isEqualTo("OFF");
        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.OFF);
        assertThat(reading.powerWatt()).isLessThanOrEqualTo(config.standbyMaxWatt());
    }

    @Test
    void dryerNeverEmitsWashingMachineOrDishwasherOnlyStages() {
        ApplianceConfig config = dryerConfig();

        ApplianceRuntimeState state = new ApplianceRuntimeState(ApplianceOperatingState.ACTIVE, "HEATING",
                MORNING.toInstant(), null, null, null, null, MORNING.toInstant(), null, null, null, 0,
                MORNING.toLocalDate());

        for (int tick = 0; tick < 900; tick++) { // 4500s, longer than the whole ~65 min cycle
            var reading = MODEL.generate(config, state, MORNING.plusSeconds(5L * (tick + 1)), Duration.ofSeconds(5),
                    () -> 0.5);
            assertThat(reading.nextState().operatingMode()).isNotIn("WASHING", "RINSING", "SPINNING", "PRE_RINSE",
                    "DRYING");
            state = reading.nextState();
        }
    }

    @Test
    void dryerAntiCreaseEndingNeverEmitsActiveStateWithOffMode() {
        ApplianceConfig config = dryerConfig();

        // Force straight into ANTI_CREASE already past its threshold, so the very next tick
        // transitions to OFF.
        ApplianceRuntimeState antiCrease = new ApplianceRuntimeState(ApplianceOperatingState.ACTIVE, "ANTI_CREASE",
                MORNING.toInstant().minusSeconds(301), null, null, null, null, MORNING.toInstant(), null, null, null,
                0, MORNING.toLocalDate());

        var reading = MODEL.generate(config, antiCrease, MORNING.plusSeconds(5), Duration.ofSeconds(5), () -> 0.5);

        assertThat(reading.nextState().operatingMode()).isEqualTo("OFF");
        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.OFF);
        assertThat(reading.powerWatt()).isLessThanOrEqualTo(config.standbyMaxWatt());
    }

    @Test
    void unrecognizedTypeFallsBackToWashingMachineModel() {
        ApplianceConfig config = new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "SOME_FUTURE_TYPE",
                new BigDecimal("2300"), new BigDecimal("300"), new BigDecimal("2200"), "SOME_FUTURE_TYPE",
                "PROGRAM_CYCLE", null, new BigDecimal("3"));

        var reading = MODEL.generate(config, null, MORNING, Duration.ofSeconds(5), () -> 0.5);

        assertThat(reading.nextState().operatingMode()).isIn("OFF", "WASHING", "RINSING", "SPINNING");
    }
}
