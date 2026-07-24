package com.vegawatt.core.common.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vegawatt.anomaly")
public record AnomalyProperties(int breachThreshold, int recoveryThreshold, BigDecimal recoveryRatio) {
}
