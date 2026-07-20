package com.vegawatt.sensors.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vegawatt.kafka")
public record SensorsKafkaProperties(String registrationTopic, String telemetryTopic) {
}
