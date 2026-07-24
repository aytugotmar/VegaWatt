package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VariableLoadBehaviorModelTest {

    private static final ZonedDateTime EVENING = ZonedDateTime.now().withHour(20).withMinute(0).withSecond(0);
    private static final VariableLoadBehaviorModel MODEL = new VariableLoadBehaviorModel();

    private static ApplianceConfig gamingComputerConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "GAMING_COMPUTER", new BigDecimal("650"),
                new BigDecimal("80"), new BigDecimal("600"), "GAMING_COMPUTER", "VARIABLE_LOAD", null,
                new BigDecimal("10"));
    }

    @Test
    void doesNotFlickerBeforeTheDwellPeriodExpires() {
        ApplianceConfig config = gamingComputerConfig();

        var first = MODEL.generate(config, null, EVENING, Duration.ofSeconds(5), () -> 0.0); // forces active
        assertThat(first.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);

        var second = MODEL.generate(config, first.nextState(), EVENING.plusMinutes(5), Duration.ofSeconds(5),
                () -> 1.0);

        assertThat(second.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);
    }

    @Test
    void standbyPowerNeverExceedsTheStandbyCeiling() {
        ApplianceConfig config = gamingComputerConfig();

        var reading = MODEL.generate(config, null, EVENING, Duration.ofSeconds(5), () -> 1.0); // forces standby

        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.STANDBY);
        assertThat(reading.powerWatt()).isLessThanOrEqualTo(config.standbyMaxWatt());
    }

    @Test
    void activePowerFluctuatesWithinTheSimulationRange() {
        ApplianceConfig config = gamingComputerConfig();

        var reading = MODEL.generate(config, null, EVENING, Duration.ofSeconds(5), () -> 0.0);

        assertThat(reading.powerWatt()).isBetween(config.simulationMinWatt(), config.simulationMaxWatt());
    }
}
