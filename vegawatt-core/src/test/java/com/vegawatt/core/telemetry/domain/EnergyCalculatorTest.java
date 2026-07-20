package com.vegawatt.core.telemetry.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EnergyCalculatorTest {

    @Test
    void convertsWattsAndSecondsToKwh() {
        BigDecimal result = EnergyCalculator.incrementKwh(new BigDecimal("1000"), 3600);

        assertThat(result).isEqualByComparingTo("1.0000");
    }

    @Test
    void convertsFractionalIntervalCorrectly() {
        BigDecimal result = EnergyCalculator.incrementKwh(new BigDecimal("1450.5"), 5);

        assertThat(result).isEqualByComparingTo("0.0020");
    }

    @Test
    void zeroPowerProducesZeroEnergy() {
        BigDecimal result = EnergyCalculator.incrementKwh(BigDecimal.ZERO, 5);

        assertThat(result).isEqualByComparingTo("0.0000");
    }
}
