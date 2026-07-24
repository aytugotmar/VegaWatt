package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManualSwitchBehaviorModelTest {

    private static final ZonedDateTime EVENING = ZonedDateTime.now().withHour(20).withMinute(0).withSecond(0);
    private static final ManualSwitchBehaviorModel MODEL = new ManualSwitchBehaviorModel();

    private static ApplianceConfig deskLampConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "DESK_LAMP", new BigDecimal("40"),
                new BigDecimal("5"), new BigDecimal("15"), "DESK_LAMP", "MANUAL_SWITCH", null, null);
    }

    @Test
    void doesNotFlickerBeforeTheDwellPeriodExpires() {
        ApplianceConfig config = deskLampConfig();

        // A losing roll (1.0) would normally force OFF, and a winning roll (0.0) would force ON —
        // the first tick's dwell decision should hold regardless of what later ticks roll.
        var first = MODEL.generate(config, null, EVENING, Duration.ofSeconds(5), () -> 0.0);
        boolean wasActive = first.nextState().operatingState() == ApplianceOperatingState.ACTIVE;

        var second = MODEL.generate(config, first.nextState(), EVENING.plusSeconds(5), Duration.ofSeconds(5),
                () -> wasActive ? 1.0 : 0.0);
        var third = MODEL.generate(config, second.nextState(), EVENING.plusMinutes(5), Duration.ofSeconds(5),
                () -> wasActive ? 1.0 : 0.0);

        assertThat(second.nextState().operatingState()).isEqualTo(first.nextState().operatingState());
        assertThat(third.nextState().operatingState()).isEqualTo(first.nextState().operatingState());
    }

    @Test
    void reevaluatesOnceTheDwellPeriodHasExpired() {
        ApplianceConfig config = deskLampConfig();

        var first = MODEL.generate(config, null, EVENING, Duration.ofSeconds(5), () -> 0.0); // forces ON
        assertThat(first.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);

        // Desk lamp dwell is at most 90 minutes — 100 minutes later the dwell must have expired,
        // so a losing roll (1.0) should now be able to force OFF.
        var later = MODEL.generate(config, first.nextState(), EVENING.plusMinutes(100), Duration.ofSeconds(5),
                () -> 1.0);

        assertThat(later.nextState().operatingState()).isEqualTo(ApplianceOperatingState.OFF);
    }

    @Test
    void offPowerNeverExceedsTheStandbyCeiling() {
        ApplianceConfig config = new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "FAN", new BigDecimal("60"),
                new BigDecimal("10"), new BigDecimal("50"), "FAN", "MANUAL_SWITCH", null, new BigDecimal("2"));

        var reading = MODEL.generate(config, null, EVENING, Duration.ofSeconds(5), () -> 1.0); // forces OFF

        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.OFF);
        assertThat(reading.powerWatt()).isLessThanOrEqualTo(config.standbyMaxWatt());
    }
}
