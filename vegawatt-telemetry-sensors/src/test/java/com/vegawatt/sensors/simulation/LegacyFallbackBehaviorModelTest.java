package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Regression coverage for the fallback state-freeze bug: before this fix, any appliance routed
 * here (an unrecognized/unparseable behavior profile) reported OFF/STANDBY forever regardless of
 * how much real power it was drawing — {@code operatingState}/{@code operatingMode} must instead
 * track the actually-computed {@code powerWatt}. */
class LegacyFallbackBehaviorModelTest {

    private static final ZonedDateTime NOON = ZonedDateTime.now().withHour(12).withMinute(0).withSecond(0);
    private static final LegacyFallbackBehaviorModel MODEL = new LegacyFallbackBehaviorModel();

    private static ApplianceConfig configWithType(String type) {
        // A very high min/max forces TelemetryGenerator to always produce power above the standby
        // ceiling, regardless of its own internal randomness/diurnal shaping.
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), type, new BigDecimal("500"),
                new BigDecimal("300"), new BigDecimal("400"), null, "FLOW_TRIGGERED", null, new BigDecimal("5"));
    }

    @Test
    void neverFreezesAtOffStandbyWhileDrawingRealPower() {
        ApplianceConfig config = configWithType("SOME_UNMODELED_TYPE");

        var first = MODEL.generate(config, null, NOON, Duration.ofSeconds(5), () -> 0.5);
        var second = MODEL.generate(config, first.nextState(), NOON.plusSeconds(5), Duration.ofSeconds(5), () -> 0.5);
        var third = MODEL.generate(config, second.nextState(), NOON.plusSeconds(10), Duration.ofSeconds(5), () -> 0.5);

        // Power is well above the standby ceiling (5W) every tick, per configWithType's ranges —
        // the old bug would have kept reporting OFF/STANDBY across all three ticks regardless.
        assertThat(first.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);
        assertThat(second.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);
        assertThat(third.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);
        assertThat(first.nextState().operatingMode()).isEqualTo("RUNNING");
    }

    @Test
    void reportsStandbyWhenPowerIsPositiveButAtOrBelowTheStandbyCeiling() {
        ApplianceConfig config = new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "SOME_UNMODELED_TYPE",
                new BigDecimal("10"), new BigDecimal("1"), new BigDecimal("2"), null, "FLOW_TRIGGERED", null,
                new BigDecimal("5"));

        var reading = MODEL.generate(config, null, NOON, Duration.ofSeconds(5), () -> 0.5);

        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.STANDBY);
        assertThat(reading.nextState().operatingMode()).isEqualTo("STANDBY");
    }
}
