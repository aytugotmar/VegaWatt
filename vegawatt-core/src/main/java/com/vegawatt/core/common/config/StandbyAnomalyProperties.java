package com.vegawatt.core.common.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vegawatt.standby-anomaly")
public record StandbyAnomalyProperties(
        BigDecimal thresholdMultiplier,
        BigDecimal minimumExcessWatt,
        int breachThreshold,
        int recoveryThreshold) {
}
