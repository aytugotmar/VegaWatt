package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScheduledSwitchBehaviorModelTest {

    private static final ScheduledSwitchBehaviorModel MODEL = new ScheduledSwitchBehaviorModel();
    private static final RandomSource MID_RANGE = () -> 0.5;

    private static ApplianceConfig gardenLightConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "GARDEN_LIGHTING", new BigDecimal("60"),
                new BigDecimal("30"), new BigDecimal("50"), "GARDEN_LIGHTING", "SCHEDULED_SWITCH", null,
                BigDecimal.ZERO);
    }

    @Test
    void isActiveDuringNightHours() {
        ZonedDateTime midnight = ZonedDateTime.now().withHour(23).withMinute(0).withSecond(0);
        ApplianceConfig config = gardenLightConfig();

        var reading = MODEL.generate(config, null, midnight, Duration.ofSeconds(5), MID_RANGE);

        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);
        assertThat(reading.nextState().operatingMode()).isEqualTo("SCHEDULED_ON");
    }

    @Test
    void isOffDuringDayHours() {
        ZonedDateTime noon = ZonedDateTime.now().withHour(12).withMinute(0).withSecond(0);
        ApplianceConfig config = gardenLightConfig();

        var reading = MODEL.generate(config, null, noon, Duration.ofSeconds(5), MID_RANGE);

        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.OFF);
        assertThat(reading.nextState().operatingMode()).isEqualTo("SCHEDULED_OFF");
        assertThat(reading.powerWatt()).isLessThanOrEqualTo(config.standbyMaxWatt());
    }
}
