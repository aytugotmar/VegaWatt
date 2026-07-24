package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShortSessionBehaviorModelTest {

    private static final ZonedDateTime DAY_START = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0)
            .withNano(0);
    private static final double WINDOW_START_HOUR = 0;
    private static final double WINDOW_END_HOUR = 24;
    private static final int MIN_DAILY_COUNT = 1;
    private static final int MAX_DAILY_COUNT = 3;
    private static final ShortSessionBehaviorModel MODEL = new ShortSessionBehaviorModel();

    private static ApplianceConfig humidifierConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "HUMIDIFIER", new BigDecimal("40"),
                new BigDecimal("10"), new BigDecimal("30"), "HUMIDIFIER", "SHORT_SESSION", null, new BigDecimal("2"));
    }

    /** The clock instant when this config's first (index-0) session is scheduled to start today —
     * derived from the same {@link DiurnalCurve#plannedSessionStartHour} the model itself calls,
     * so the test stays correct regardless of the random appliance id or the day it runs on. */
    private static ZonedDateTime firstPlannedSessionStart(ApplianceConfig config) {
        double hour = DiurnalCurve.plannedSessionStartHour(DAY_START, config.applianceId(), 0, WINDOW_START_HOUR,
                WINDOW_END_HOUR, MIN_DAILY_COUNT, MAX_DAILY_COUNT);
        // +1s margin: Math.round can land a fraction of a second before the threshold, which would
        // make the model's own fractionalHour(measuredAt) >= plannedStart check fail by an epsilon.
        return DAY_START.plusSeconds(Math.round(hour * 3600) + 1);
    }

    @Test
    void aStartedSessionHoldsActiveAcrossManyTicksWithoutRerolling() {
        ApplianceConfig config = humidifierConfig();
        ZonedDateTime plannedStart = firstPlannedSessionStart(config);

        var first = MODEL.generate(config, null, plannedStart, Duration.ofSeconds(5), () -> 0.0); // starts a session
        assertThat(first.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);

        // 30 minutes later, well inside even the shortest possible 1h session — a losing roll (1.0)
        // must not be able to force it off early.
        var later = MODEL.generate(config, first.nextState(), plannedStart.plusMinutes(30), Duration.ofSeconds(5),
                () -> 1.0);

        assertThat(later.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);
    }

    @Test
    void offPowerNeverExceedsTheStandbyCeiling() {
        ApplianceConfig config = humidifierConfig();
        ZonedDateTime justBeforePlannedStart = firstPlannedSessionStart(config).minusSeconds(5);

        var reading = MODEL.generate(config, null, justBeforePlannedStart, Duration.ofSeconds(5), () -> 1.0);

        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.OFF);
        assertThat(reading.powerWatt()).isLessThanOrEqualTo(config.standbyMaxWatt());
    }

    @Test
    void neverStartsMoreSessionsThanTheDailyCap() {
        ApplianceConfig config = humidifierConfig();
        ApplianceRuntimeState state = null;
        int sessionsStarted = 0;

        int ticks = (int) Math.ceil(24 * 3600 / 5.0); // whole day, so every planned session can fire
        for (int tick = 0; tick < ticks; tick++) {
            ZonedDateTime measuredAt = DAY_START.plusSeconds(5L * tick);
            var reading = MODEL.generate(config, state, measuredAt, Duration.ofSeconds(5), () -> 0.0);
            boolean sessionJustStarted = (state == null || state.operatingState() != ApplianceOperatingState.ACTIVE)
                    && reading.nextState().operatingState() == ApplianceOperatingState.ACTIVE;
            if (sessionJustStarted) {
                sessionsStarted++;
            }
            state = reading.nextState();
        }

        assertThat(sessionsStarted).isLessThanOrEqualTo(MAX_DAILY_COUNT);
    }
}
