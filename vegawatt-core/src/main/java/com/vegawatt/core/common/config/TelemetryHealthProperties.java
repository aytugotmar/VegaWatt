package com.vegawatt.core.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vegawatt.telemetry-health")
public record TelemetryHealthProperties(int staleAfterSeconds, int offlineAfterSeconds, int sweepIntervalSeconds) {
}
