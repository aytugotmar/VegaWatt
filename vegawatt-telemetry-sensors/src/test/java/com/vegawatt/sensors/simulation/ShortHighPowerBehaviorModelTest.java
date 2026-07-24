package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShortHighPowerBehaviorModelTest {

    private static final ZonedDateTime MORNING = ZonedDateTime.now().withHour(9).withMinute(0).withSecond(0);
    private static final ShortHighPowerBehaviorModel MODEL = new ShortHighPowerBehaviorModel();

    private static ApplianceConfig kettleConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "KETTLE", new BigDecimal("2200"),
                new BigDecimal("1800"), new BigDecimal("2100"), "KETTLE", "SHORT_HIGH_POWER", null,
                new BigDecimal("1"));
    }

    @Test
    void offPowerNeverExceedsTheStandbyCeiling() {
        ApplianceConfig config = kettleConfig();

        // A losing roll (1.0) never starts a session, so it should stay OFF this very first tick.
        var reading = MODEL.generate(config, null, MORNING, Duration.ofSeconds(5), () -> 1.0);

        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.OFF);
        assertThat(reading.powerWatt()).isLessThanOrEqualTo(config.standbyMaxWatt());
    }

    @Test
    void aStartedSessionHoldsActiveForItsFullDurationWithoutPerTickReroll() {
        ApplianceConfig config = kettleConfig();

        // Winning roll (0.0) starts a session on the very first tick.
        var first = MODEL.generate(config, null, MORNING, Duration.ofSeconds(5), () -> 0.0);
        assertThat(first.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);

        // A losing roll (1.0) supplied for every subsequent tick would force a stop under the old
        // per-tick-reroll logic, but the session should hold until its own randomly assigned end.
        var second = MODEL.generate(config, first.nextState(), MORNING.plusSeconds(10), Duration.ofSeconds(5),
                () -> 1.0);

        assertThat(second.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);
    }

    @Test
    void neverStartsMoreSessionsThanTheDailyCap() {
        ApplianceConfig config = kettleConfig();
        ApplianceRuntimeState state = null;
        int sessionsStarted = 0;

        // Winning roll (0.0) every tick would start a new session immediately after each one ends,
        // for the whole day — the daily cap must still hold.
        for (int tick = 0; tick < 2000; tick++) {
            ZonedDateTime measuredAt = MORNING.plusSeconds(5L * tick);
            var reading = MODEL.generate(config, state, measuredAt, Duration.ofSeconds(5), () -> 0.0);
            boolean sessionJustStarted = state == null
                    || (state.operatingState() != ApplianceOperatingState.ACTIVE
                            && reading.nextState().operatingState() == ApplianceOperatingState.ACTIVE);
            if (sessionJustStarted) {
                sessionsStarted++;
            }
            state = reading.nextState();
        }

        assertThat(sessionsStarted).isLessThanOrEqualTo(8); // KETTLE's daily cap
    }
}
