package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlwaysOnVariableBehaviorModelTest {

    private static final ZonedDateTime NOON = ZonedDateTime.now().withHour(12).withMinute(0).withSecond(0);
    private static final AlwaysOnVariableBehaviorModel MODEL = new AlwaysOnVariableBehaviorModel();

    private static ApplianceConfig airPurifierConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "AIR_PURIFIER", new BigDecimal("50"),
                new BigDecimal("10"), new BigDecimal("40"), "AIR_PURIFIER", "ALWAYS_ON_VARIABLE", null, null);
    }

    @Test
    void isAlwaysActiveNeverOffOrStandby() {
        ApplianceConfig config = airPurifierConfig();

        var reading = MODEL.generate(config, null, NOON, Duration.ofSeconds(5), () -> 0.5);

        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);
        assertThat(reading.powerWatt()).isBetween(config.simulationMinWatt(), config.simulationMaxWatt());
    }

    @Test
    void powerVariesAcrossTicksRatherThanStayingConstant() {
        ApplianceConfig config = airPurifierConfig();

        var low = MODEL.generate(config, null, NOON, Duration.ofSeconds(5), () -> 0.0);
        var high = MODEL.generate(config, low.nextState(), NOON.plusSeconds(5), Duration.ofSeconds(5), () -> 1.0);

        assertThat(low.powerWatt()).isEqualByComparingTo(config.simulationMinWatt());
        assertThat(high.powerWatt()).isEqualByComparingTo(config.simulationMaxWatt());
    }
}
