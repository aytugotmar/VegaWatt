package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShortHighPowerBehaviorModelTest {

    private static final ZonedDateTime DAY_START = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0)
            .withNano(0);
    private static final double WINDOW_START_HOUR = 6;
    private static final double WINDOW_END_HOUR = 23;
    private static final int MIN_DAILY_COUNT = 3;
    private static final int MAX_DAILY_COUNT = 8;
    private static final ShortHighPowerBehaviorModel MODEL = new ShortHighPowerBehaviorModel();

    private static ApplianceConfig kettleConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "KETTLE", new BigDecimal("2200"),
                new BigDecimal("1800"), new BigDecimal("2100"), "KETTLE", "SHORT_HIGH_POWER", null,
                new BigDecimal("1"));
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
    void offPowerNeverExceedsTheStandbyCeiling() {
        ApplianceConfig config = kettleConfig();
        ZonedDateTime justBeforePlannedStart = firstPlannedSessionStart(config).minusSeconds(5);

        // A losing roll (1.0) can no longer matter for session scheduling (that's now deterministic
        // by time+appliance id), only for in-session jitter — the check here is that the model
        // stays OFF until its own planned start time.
        var reading = MODEL.generate(config, null, justBeforePlannedStart, Duration.ofSeconds(5), () -> 1.0);

        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.OFF);
        assertThat(reading.powerWatt()).isLessThanOrEqualTo(config.standbyMaxWatt());
    }

    @Test
    void aStartedSessionHoldsActiveForItsFullDurationWithoutPerTickReroll() {
        ApplianceConfig config = kettleConfig();
        ZonedDateTime plannedStart = firstPlannedSessionStart(config);

        var first = MODEL.generate(config, null, plannedStart, Duration.ofSeconds(5), () -> 0.0);
        assertThat(first.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);

        // A losing roll (1.0) supplied for every subsequent tick would force a stop under the old
        // per-tick-reroll logic, but the session should hold until its own randomly assigned end.
        var second = MODEL.generate(config, first.nextState(), plannedStart.plusSeconds(10), Duration.ofSeconds(5),
                () -> 1.0);

        assertThat(second.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);
    }

    @Test
    void neverStartsMoreSessionsThanTheDailyCap() {
        ApplianceConfig config = kettleConfig();
        ApplianceRuntimeState state = null;
        int sessionsStarted = 0;

        // Run across the appliance's whole daytime window so every planned session gets a chance
        // to fire — the daily cap must still hold.
        int ticks = (int) Math.ceil(24 * 3600 / 5.0);
        for (int tick = 0; tick < ticks; tick++) {
            ZonedDateTime measuredAt = DAY_START.plusSeconds(5L * tick);
            var reading = MODEL.generate(config, state, measuredAt, Duration.ofSeconds(5), () -> 0.0);
            // "state == null" only means "tick 0", not "a session started" — the device could
            // equally be OFF at tick 0 (the common case, since DAY_START is midnight and the
            // window doesn't open until WINDOW_START_HOUR). Only count it when tick 0 itself
            // lands inside an ACTIVE session; the old unconditional "state == null" branch
            // over-counted by exactly one on any day whose planned session count reached the cap,
            // intermittently failing this assertion by one.
            boolean sessionJustStarted = reading.nextState().operatingState() == ApplianceOperatingState.ACTIVE
                    && (state == null || state.operatingState() != ApplianceOperatingState.ACTIVE);
            if (sessionJustStarted) {
                sessionsStarted++;
            }
            state = reading.nextState();
        }

        assertThat(sessionsStarted).isLessThanOrEqualTo(MAX_DAILY_COUNT);
    }
}
