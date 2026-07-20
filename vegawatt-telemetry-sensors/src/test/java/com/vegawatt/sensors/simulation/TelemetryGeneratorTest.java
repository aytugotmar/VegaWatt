package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TelemetryGeneratorTest {

    private static final ApplianceConfig CONFIG = new ApplianceConfig(
            UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("2000"), new BigDecimal("100"),
            new BigDecimal("1800"));

    @Test
    void staysWithinSimulationRangeWhenNotSpiking() {
        RandomSource midRange = () -> 0.5;

        BigDecimal reading = TelemetryGenerator.generatePowerWatt(CONFIG, midRange);

        assertThat(reading).isEqualByComparingTo("950.00");
    }

    @Test
    void producesMinimumOfRangeAtLowerBound() {
        RandomSource noSpikeThenLowerBound = sequenceOf(0.5, 0.0);

        BigDecimal reading = TelemetryGenerator.generatePowerWatt(CONFIG, noSpikeThenLowerBound);

        assertThat(reading).isEqualByComparingTo(CONFIG.simulationMinWatt());
    }

    @Test
    void spikeReadingExceedsSafePowerLimit() {
        RandomSource alwaysSpikes = () -> 0.01;

        BigDecimal reading = TelemetryGenerator.generatePowerWatt(CONFIG, alwaysSpikes);

        assertThat(reading).isGreaterThanOrEqualTo(CONFIG.safePowerLimitWatt());
    }

    @Test
    void neverGeneratesNegativePower() {
        ApplianceConfig zeroFloorConfig = new ApplianceConfig(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("500"), BigDecimal.ZERO,
                new BigDecimal("400"));
        RandomSource noSpikeThenLowerBound = sequenceOf(0.5, 0.0);

        BigDecimal reading = TelemetryGenerator.generatePowerWatt(zeroFloorConfig, noSpikeThenLowerBound);

        assertThat(reading).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private static RandomSource sequenceOf(double... values) {
        AtomicInteger index = new AtomicInteger(0);
        return () -> values[index.getAndIncrement()];
    }
}
