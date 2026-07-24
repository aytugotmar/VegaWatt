package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Covers all 4 device-family dispatch targets — refrigeration keeps its existing
 * compressor/defrost modes, the other 3 families get device-appropriate mode names instead of a
 * one-size-fits-all thermostat cycle. */
class ThermostaticCycleBehaviorModelTest {

    private static final ZonedDateTime NOON = ZonedDateTime.now().withHour(12).withMinute(0).withSecond(0);
    private static final ThermostaticCycleBehaviorModel MODEL = new ThermostaticCycleBehaviorModel();

    private static ApplianceConfig configFor(String type) {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), type, new BigDecimal("2500"),
                new BigDecimal("40"), new BigDecimal("2000"), type, "THERMOSTATIC_CYCLE", null, null);
    }

    private static ApplianceConfig configForWithStandby(String type, String standbyMin, String standbyMax) {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), type, new BigDecimal("2500"),
                new BigDecimal("40"), new BigDecimal("2000"), type, "THERMOSTATIC_CYCLE",
                new BigDecimal(standbyMin), new BigDecimal(standbyMax));
    }

    @Test
    void refrigeratorUsesCompressorAndDefrostModes() {
        var reading = MODEL.generate(configFor("REFRIGERATOR"), null, NOON, Duration.ofSeconds(5), () -> 0.5);

        assertThat(reading.nextState().operatingMode()).isIn("COMPRESSOR_ON", "IDLE", "DEFROST");
    }

    @Test
    void airConditionerUsesCoolingModeNotCompressorOrDefrost() {
        var reading = MODEL.generate(configFor("AIR_CONDITIONER"), null, NOON, Duration.ofSeconds(5), () -> 0.5);

        assertThat(reading.nextState().operatingMode()).isIn("COOLING", "IDLE");
    }

    @Test
    void electricHeaterUsesHeatingModeNotCompressorOrDefrost() {
        var reading = MODEL.generate(configFor("ELECTRIC_HEATER"), null, NOON, Duration.ofSeconds(5), () -> 0.5);

        assertThat(reading.nextState().operatingMode()).isIn("HEATING", "IDLE");
    }

    @Test
    void fanHeaterUsesHeatingModeNotCompressorOrDefrost() {
        var reading = MODEL.generate(configFor("FAN_HEATER"), null, NOON, Duration.ofSeconds(5), () -> 0.5);

        assertThat(reading.nextState().operatingMode()).isIn("HEATING", "IDLE");
    }

    @Test
    void waterHeaterUsesHeatingModeWithALongerCycleThanARoomHeater() {
        ApplianceConfig config = configFor("WATER_HEATER");

        // Force straight into HEATING already 700s in — long enough to still be heating for a room
        // heater's shorter 600s threshold check but this is a water heater, which should hold
        // HEATING far longer (1800s threshold).
        ApplianceRuntimeState heating = new ApplianceRuntimeState(ApplianceOperatingState.ACTIVE, "HEATING",
                NOON.toInstant().minusSeconds(700), null, null, null, null, NOON.toInstant(), null, null, null, 0,
                NOON.toLocalDate());

        var reading = MODEL.generate(config, heating, NOON.plusSeconds(5), Duration.ofSeconds(5), () -> 1.0);

        assertThat(reading.nextState().operatingMode()).isEqualTo("HEATING");
    }

    @Test
    void unrecognizedTypeFallsBackToRefrigerationModel() {
        var reading = MODEL.generate(configFor("SOME_FUTURE_TYPE"), null, NOON, Duration.ofSeconds(5), () -> 0.5);

        assertThat(reading.nextState().operatingMode()).isIn("COMPRESSOR_ON", "IDLE", "DEFROST");
    }

    @Test
    void idlePowerUsesTheStandbyRangeNotTheActiveMinimum() {
        ApplianceConfig config = configForWithStandby("REFRIGERATOR", "2", "5");
        ApplianceRuntimeState idle = new ApplianceRuntimeState(ApplianceOperatingState.STANDBY, "IDLE",
                NOON.toInstant(), null, null, null, null, NOON.toInstant(), null, null, null, 0, NOON.toLocalDate());

        // Only 5s elapsed since entering IDLE, well under the 1200s minimum dwell, so this stays
        // IDLE regardless of the random roll.
        var reading = MODEL.generate(config, idle, NOON.plusSeconds(5), Duration.ofSeconds(5), () -> 0.5);

        assertThat(reading.nextState().operatingMode()).isEqualTo("IDLE");
        assertThat(reading.powerWatt()).isBetween(new BigDecimal("2"), new BigDecimal("5"));
    }

    @Test
    void airConditionerCanTakeARealOffExcursionAtLowOvernightDemand() {
        ApplianceConfig config = configForWithStandby("AIR_CONDITIONER", "1", "3");
        ZonedDateTime trough = ZonedDateTime.now().withHour(3).withMinute(30).withSecond(0).withNano(0);
        ApplianceRuntimeState idle = new ApplianceRuntimeState(ApplianceOperatingState.STANDBY, "IDLE",
                trough.toInstant().minusSeconds(1201), null, null, null, null, trough.toInstant(), null, null, null,
                0, trough.toLocalDate());

        var reading = MODEL.generate(config, idle, trough, Duration.ofSeconds(5), () -> 0.0);

        assertThat(reading.nextState().operatingMode()).isEqualTo("OFF");
        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.OFF);
    }

    @Test
    void roomHeaterCanTakeARealOffExcursionAtLowMiddayDemand() {
        ApplianceConfig config = configForWithStandby("ELECTRIC_HEATER", "1", "3");
        ZonedDateTime trough = ZonedDateTime.now().withHour(13).withMinute(30).withSecond(0).withNano(0);
        ApplianceRuntimeState idle = new ApplianceRuntimeState(ApplianceOperatingState.STANDBY, "IDLE",
                trough.toInstant().minusSeconds(901), null, null, null, null, trough.toInstant(), null, null, null,
                0, trough.toLocalDate());

        var reading = MODEL.generate(config, idle, trough, Duration.ofSeconds(5), () -> 0.0);

        assertThat(reading.nextState().operatingMode()).isEqualTo("OFF");
        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.OFF);
    }

    @Test
    void refrigeratorNeverTakesAnOffExcursion() {
        ApplianceConfig config = configForWithStandby("REFRIGERATOR", "1", "3");
        ZonedDateTime anyTime = ZonedDateTime.now().withHour(3).withMinute(30).withSecond(0).withNano(0);
        ApplianceRuntimeState idle = new ApplianceRuntimeState(ApplianceOperatingState.STANDBY, "IDLE",
                anyTime.toInstant().minusSeconds(1300), null, null, null, null, anyTime.toInstant(), null, null,
                null, 0, anyTime.toLocalDate());

        var reading = MODEL.generate(config, idle, anyTime, Duration.ofSeconds(5), () -> 0.0);

        assertThat(reading.nextState().operatingMode()).isNotEqualTo("OFF");
    }
}
