package com.vegawatt.core.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vegawatt.kafka")
public record VegaWattKafkaProperties(String registrationTopic, String telemetryTopic) {
}
