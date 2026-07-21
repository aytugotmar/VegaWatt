package com.vegawatt.core.telemetry.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class EnergyCalculator {

    private static final int ENERGY_SCALE = 9;
    private static final BigDecimal WATT_SECONDS_PER_KWH = new BigDecimal("3600000");

    private EnergyCalculator() {
    }

    public static BigDecimal incrementKwh(BigDecimal powerWatt, int measurementIntervalSeconds) {
        return powerWatt.multiply(BigDecimal.valueOf(measurementIntervalSeconds))
                .divide(WATT_SECONDS_PER_KWH, ENERGY_SCALE, RoundingMode.HALF_UP);
    }
}
