package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShortSessionBehaviorModelTest {

    private static final ZonedDateTime MORNING = ZonedDateTime.now().withHour(9).withMinute(0).withSecond(0);
    private static final ShortSessionBehaviorModel MODEL = new ShortSessionBehaviorModel();

    private static ApplianceConfig humidifierConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "HUMIDIFIER", new BigDecimal("40"),
                new BigDecimal("10"), new BigDecimal("30"), "HUMIDIFIER", "SHORT_SESSION", null, new BigDecimal("2"));
    }

    @Test
    void aStartedSessionHoldsActiveAcrossManyTicksWithoutRerolling() {
        ApplianceConfig config = humidifierConfig();

        var first = MODEL.generate(config, null, MORNING, Duration.ofSeconds(5), () -> 0.0); // starts a session
        assertThat(first.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);

        // 30 minutes later, well inside even the shortest possible 1h session — a losing roll (1.0)
        // must not be able to force it off early.
        var later = MODEL.generate(config, first.nextState(), MORNING.plusMinutes(30), Duration.ofSeconds(5),
                () -> 1.0);

        assertThat(later.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);
    }

    @Test
    void offPowerNeverExceedsTheStandbyCeiling() {
        ApplianceConfig config = humidifierConfig();

        var reading = MODEL.generate(config, null, MORNING, Duration.ofSeconds(5), () -> 1.0); // never starts

        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.OFF);
        assertThat(reading.powerWatt()).isLessThanOrEqualTo(config.standbyMaxWatt());
    }

    @Test
    void neverStartsMoreSessionsThanTheDailyCap() {
        ApplianceConfig config = humidifierConfig();
        ApplianceRuntimeState state = null;
        int sessionsStarted = 0;

        for (int tick = 0; tick < 8000; tick++) { // ~11h, stays within one calendar day
            ZonedDateTime measuredAt = MORNING.plusSeconds(5L * tick);
            var reading = MODEL.generate(config, state, measuredAt, Duration.ofSeconds(5), () -> 0.0);
            boolean sessionJustStarted = (state == null || state.operatingState() != ApplianceOperatingState.ACTIVE)
                    && reading.nextState().operatingState() == ApplianceOperatingState.ACTIVE;
            if (sessionJustStarted) {
                sessionsStarted++;
            }
            state = reading.nextState();
        }

        assertThat(sessionsStarted).isLessThanOrEqualTo(3); // HUMIDIFIER's daily cap
    }
}
