package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Covers both device-family dispatch targets — an oven keeps its PREHEATING/BAKING/KEEP_WARM
 * stages, while an iron gets its own much shorter HEATING/IRONING/THERMOSTAT_IDLE cycle instead of
 * the oven's stage names (an iron never "bakes" or "keeps warm"). */
class ThermostaticSessionBehaviorModelTest {

    private static final ZonedDateTime MORNING = ZonedDateTime.now().withHour(9).withMinute(0).withSecond(0);
    private static final ThermostaticSessionBehaviorModel MODEL = new ThermostaticSessionBehaviorModel();

    private static ApplianceConfig ovenConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "OVEN", new BigDecimal("2600"),
                new BigDecimal("500"), new BigDecimal("2500"), "OVEN", "THERMOSTATIC_SESSION", null,
                new BigDecimal("3"));
    }

    private static ApplianceConfig ironConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "IRON", new BigDecimal("1800"),
                new BigDecimal("50"), new BigDecimal("1600"), "IRON", "THERMOSTATIC_SESSION", null,
                new BigDecimal("2"));
    }

    @Test
    void keepWarmEndingNeverEmitsActiveStateWithOffMode() {
        ApplianceConfig config = ovenConfig();

        // Force straight into KEEP_WARM already 601s in (past its 600s threshold).
        ApplianceRuntimeState keepWarm = new ApplianceRuntimeState(ApplianceOperatingState.ACTIVE, "KEEP_WARM",
                MORNING.toInstant().minusSeconds(601), null, null, null, null, MORNING.toInstant(), null, null, null,
                0, MORNING.toLocalDate());

        var reading = MODEL.generate(config, keepWarm, MORNING.plusSeconds(5), Duration.ofSeconds(5), () -> 0.5);

        assertThat(reading.nextState().operatingMode()).isEqualTo("OFF");
        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.OFF);
        assertThat(reading.powerWatt()).isLessThanOrEqualTo(config.standbyMaxWatt());
    }

    @Test
    void neverStartsMoreSessionsThanTheDailyCap() {
        ApplianceConfig config = ovenConfig();
        ApplianceRuntimeState state = null;
        int sessionsStarted = 0;

        for (int tick = 0; tick < 8000; tick++) { // ~11h, stays within one calendar day
            ZonedDateTime measuredAt = MORNING.plusSeconds(5L * tick);
            var reading = MODEL.generate(config, state, measuredAt, Duration.ofSeconds(5), () -> 0.0);
            boolean sessionJustStarted = "OFF".equals(state == null ? "OFF" : state.operatingMode())
                    && "PREHEATING".equals(reading.nextState().operatingMode());
            if (sessionJustStarted) {
                sessionsStarted++;
            }
            state = reading.nextState();
        }

        assertThat(sessionsStarted).isLessThanOrEqualTo(3); // OVEN's daily cap
    }

    @Test
    void ironNeverEmitsOvenOnlyStages() {
        ApplianceConfig config = ironConfig();

        ApplianceRuntimeState state = new ApplianceRuntimeState(ApplianceOperatingState.ACTIVE, "HEATING",
                MORNING.toInstant(), null, null, null, null, MORNING.toInstant(), null, null, null, 0,
                MORNING.toLocalDate());

        // Walk the iron's own stage machine forward far enough to pass through every stage; it
        // should never emit BAKING or KEEP_WARM — the oven-only stages.
        for (int tick = 0; tick < 400; tick++) { // 2000s, longer than the whole ~29 min session
            var reading = MODEL.generate(config, state, MORNING.plusSeconds(5L * (tick + 1)), Duration.ofSeconds(5),
                    () -> 0.5);
            assertThat(reading.nextState().operatingMode()).isNotIn("BAKING", "KEEP_WARM", "PREHEATING");
            state = reading.nextState();
        }
    }

    @Test
    void ironThermostatIdleEndingNeverEmitsActiveStateWithOffMode() {
        ApplianceConfig config = ironConfig();

        // Force straight into THERMOSTAT_IDLE already past its threshold, so the very next tick
        // transitions to OFF.
        ApplianceRuntimeState idle = new ApplianceRuntimeState(ApplianceOperatingState.ACTIVE, "THERMOSTAT_IDLE",
                MORNING.toInstant().minusSeconds(201), null, null, null, null, MORNING.toInstant(), null, null, null,
                0, MORNING.toLocalDate());

        var reading = MODEL.generate(config, idle, MORNING.plusSeconds(5), Duration.ofSeconds(5), () -> 0.5);

        assertThat(reading.nextState().operatingMode()).isEqualTo("OFF");
        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.OFF);
        assertThat(reading.powerWatt()).isLessThanOrEqualTo(config.standbyMaxWatt());
    }

    @Test
    void unrecognizedTypeFallsBackToOvenModel() {
        ApplianceConfig config = new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "SOME_FUTURE_TYPE",
                new BigDecimal("2600"), new BigDecimal("500"), new BigDecimal("2500"), "SOME_FUTURE_TYPE",
                "THERMOSTATIC_SESSION", null, new BigDecimal("3"));

        var reading = MODEL.generate(config, null, MORNING, Duration.ofSeconds(5), () -> 0.5);

        assertThat(reading.nextState().operatingMode()).isIn("OFF", "PREHEATING", "BAKING", "KEEP_WARM");
    }
}
