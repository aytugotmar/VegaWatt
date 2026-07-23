package com.vegawatt.sensors.simulation;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vegawatt.simulation")
public record SimulationProperties(int telemetryIntervalSeconds, double faultProbability) {
}
