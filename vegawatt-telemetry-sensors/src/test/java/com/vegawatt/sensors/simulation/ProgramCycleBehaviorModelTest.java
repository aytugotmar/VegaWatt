package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProgramCycleBehaviorModelTest {

    private static final ZonedDateTime MORNING = ZonedDateTime.now().withHour(9).withMinute(0).withSecond(0);
    private static final ProgramCycleBehaviorModel MODEL = new ProgramCycleBehaviorModel();

    private static ApplianceConfig washingMachineConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "WASHING_MACHINE", new BigDecimal("2300"),
                new BigDecimal("300"), new BigDecimal("2200"), "WASHING_MACHINE", "PROGRAM_CYCLE", null,
                new BigDecimal("3"));
    }

    @Test
    void spinStageEndingNeverEmitsActiveStateWithOffMode() {
        ApplianceConfig config = washingMachineConfig();

        // Force straight into SPINNING already 301s in (past its 300s threshold), so the very next
        // tick transitions to OFF.
        ApplianceRuntimeState spinning = new ApplianceRuntimeState(ApplianceOperatingState.ACTIVE, "SPINNING",
                MORNING.toInstant().minusSeconds(301), null, null, null, null, MORNING.toInstant(), null, null, null,
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
}
