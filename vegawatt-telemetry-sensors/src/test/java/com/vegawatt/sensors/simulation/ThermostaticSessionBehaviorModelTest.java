package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ThermostaticSessionBehaviorModelTest {

    private static final ZonedDateTime MORNING = ZonedDateTime.now().withHour(9).withMinute(0).withSecond(0);
    private static final ThermostaticSessionBehaviorModel MODEL = new ThermostaticSessionBehaviorModel();

    private static ApplianceConfig ovenConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "OVEN", new BigDecimal("2600"),
                new BigDecimal("500"), new BigDecimal("2500"), "OVEN", "THERMOSTATIC_SESSION", null,
                new BigDecimal("3"));
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
}
