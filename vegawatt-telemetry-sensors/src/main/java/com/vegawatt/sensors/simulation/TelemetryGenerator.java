package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TelemetryGenerator {

    private static final double SPIKE_PROBABILITY = 0.1;
    private static final BigDecimal SPIKE_MULTIPLIER = new BigDecimal("1.5");

    private TelemetryGenerator() {
    }

    public static BigDecimal generatePowerWatt(ApplianceConfig config, RandomSource randomSource) {
        if (randomSource.nextDouble() < SPIKE_PROBABILITY) {
            return spikeReading(config, randomSource);
        }
        return normalReading(config, randomSource);
    }

    private static BigDecimal normalReading(ApplianceConfig config, RandomSource randomSource) {
        return randomInRange(config.simulationMinWatt(), config.simulationMaxWatt(), randomSource);
    }

    private static BigDecimal spikeReading(ApplianceConfig config, RandomSource randomSource) {
        BigDecimal spikeCeiling = config.safePowerLimitWatt().multiply(SPIKE_MULTIPLIER);
        return randomInRange(config.safePowerLimitWatt(), spikeCeiling, randomSource);
    }

    private static BigDecimal randomInRange(BigDecimal min, BigDecimal max, RandomSource randomSource) {
        BigDecimal span = max.subtract(min);
        BigDecimal value = min.add(span.multiply(BigDecimal.valueOf(randomSource.nextDouble())));
        return value.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
