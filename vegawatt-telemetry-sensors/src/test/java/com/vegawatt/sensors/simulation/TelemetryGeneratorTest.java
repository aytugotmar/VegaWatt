package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TelemetryGeneratorTest {

    /** hashCode() == 0, so every hash-derived phase/offset in {@link DiurnalCurve} is exactly 0. */
    private static final UUID FIXED_ID = new UUID(0L, 0L);

    /** An unmapped type, so {@link ApplianceProfiles} falls back to {@code GENERIC}. */
    private static final ApplianceConfig GENERIC_CONFIG = new ApplianceConfig(
            FIXED_ID, UUID.randomUUID(), "SPACE_HEATER", new BigDecimal("2000"), new BigDecimal("100"),
            new BigDecimal("1800"), null, null, null, null);

    private static final ApplianceConfig REFRIGERATOR_CONFIG = new ApplianceConfig(
            FIXED_ID, UUID.randomUUID(), "REFRIGERATOR", new BigDecimal("200"), new BigDecimal("100"),
            new BigDecimal("200"), null, null, null, null);

    private static final ApplianceConfig WASHING_MACHINE_CONFIG = new ApplianceConfig(
            FIXED_ID, UUID.randomUUID(), "WASHING_MACHINE", new BigDecimal("2200"), new BigDecimal("100"),
            new BigDecimal("1800"), null, null, null, null);

    /** 1970-01-01, a Thursday (weekday) — so {@code epochDay == 0} keeps session-offset math clean. */
    private static final LocalDate WEEKDAY = LocalDate.ofEpochDay(0);

    private static final RandomSource MID_RANGE = () -> 0.5;

    private static ZonedDateTime at(LocalTime time) {
        return ZonedDateTime.of(WEEKDAY, time, ZoneOffset.UTC);
    }

    @Test
    void staysWithinSimulationRangeWhenNotSpiking() {
        BigDecimal reading = TelemetryGenerator.generatePowerWatt(GENERIC_CONFIG, MID_RANGE, at(LocalTime.NOON));

        assertThat(reading).isEqualByComparingTo("950.00");
    }

    @Test
    void producesMinimumOfRangeAtLowerBound() {
        RandomSource noSpikeThenLowerBound = sequenceOf(0.5, 0.0);

        BigDecimal reading = TelemetryGenerator.generatePowerWatt(GENERIC_CONFIG, noSpikeThenLowerBound, at(LocalTime.NOON));

        assertThat(reading).isEqualByComparingTo(GENERIC_CONFIG.simulationMinWatt());
    }

    @Test
    void spikeReadingExceedsSafePowerLimit() {
        RandomSource alwaysSpikes = () -> 0.01;

        BigDecimal reading = TelemetryGenerator.generatePowerWatt(GENERIC_CONFIG, alwaysSpikes, at(LocalTime.NOON));

        assertThat(reading).isGreaterThanOrEqualTo(GENERIC_CONFIG.safePowerLimitWatt());
    }

    @Test
    void neverGeneratesNegativePower() {
        ApplianceConfig zeroFloorConfig = new ApplianceConfig(FIXED_ID, UUID.randomUUID(), "SPACE_HEATER",
                new BigDecimal("500"), BigDecimal.ZERO, new BigDecimal("400"), null, null, null, null);
        RandomSource noSpikeThenLowerBound = sequenceOf(0.5, 0.0);

        BigDecimal reading = TelemetryGenerator.generatePowerWatt(zeroFloorConfig, noSpikeThenLowerBound, at(LocalTime.NOON));

        assertThat(reading).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void appliesEveningPeakAndOvernightLowForGenericAppliances() {
        BigDecimal evening = TelemetryGenerator.generatePowerWatt(GENERIC_CONFIG, MID_RANGE, at(LocalTime.of(19, 0)));
        BigDecimal night = TelemetryGenerator.generatePowerWatt(GENERIC_CONFIG, MID_RANGE, at(LocalTime.of(3, 0)));
        BigDecimal midday = TelemetryGenerator.generatePowerWatt(GENERIC_CONFIG, MID_RANGE, at(LocalTime.NOON));

        assertThat(evening).isEqualByComparingTo("1282.50");
        assertThat(night).isEqualByComparingTo("427.50");
        assertThat(midday).isEqualByComparingTo("950.00");
    }

    @Test
    void twoAppliancesOfTheSameTypeDoNotPeakAtTheExactSameReadingAcrossHomes() {
        ApplianceConfig otherHomesAc = new ApplianceConfig(new UUID(0L, 1L), UUID.randomUUID(), "SPACE_HEATER",
                new BigDecimal("2000"), new BigDecimal("100"), new BigDecimal("1800"), null, null, null, null);

        BigDecimal fixedIdReading = TelemetryGenerator.generatePowerWatt(GENERIC_CONFIG, MID_RANGE, at(LocalTime.of(19, 0)));
        BigDecimal otherApplianceReading = TelemetryGenerator.generatePowerWatt(otherHomesAc, MID_RANGE, at(LocalTime.of(19, 0)));

        assertThat(otherApplianceReading).isNotEqualByComparingTo(fixedIdReading);
    }

    @Test
    void refrigeratorDrawsStandbyPowerWhileTheCompressorCycleIsOff() {
        ZonedDateTime onWindow = Instant.ofEpochSecond(100).atZone(ZoneOffset.UTC);
        ZonedDateTime offWindow = Instant.ofEpochSecond(800).atZone(ZoneOffset.UTC);

        BigDecimal duringOn = TelemetryGenerator.generatePowerWatt(REFRIGERATOR_CONFIG, MID_RANGE, onWindow);
        BigDecimal duringOff = TelemetryGenerator.generatePowerWatt(REFRIGERATOR_CONFIG, MID_RANGE, offWindow);

        assertThat(duringOn).isEqualByComparingTo("150.00");
        assertThat(duringOff).isEqualByComparingTo("30.00");
    }

    @Test
    void washingMachineOnlyDrawsFullPowerDuringItsDailySessionOnAWeekday() {
        BigDecimal duringSession = TelemetryGenerator.generatePowerWatt(WASHING_MACHINE_CONFIG, MID_RANGE,
                at(LocalTime.of(19, 30)));
        BigDecimal outsideSession = TelemetryGenerator.generatePowerWatt(WASHING_MACHINE_CONFIG, MID_RANGE,
                at(LocalTime.of(12, 0)));

        assertThat(duringSession).isEqualByComparingTo("950.00");
        assertThat(outsideSession).isEqualByComparingTo("28.50");
    }

    private static RandomSource sequenceOf(double... values) {
        AtomicInteger index = new AtomicInteger(0);
        return () -> values[index.getAndIncrement()];
    }
}
