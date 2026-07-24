package com.vegawatt.core.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vegawatt.notification")
public record NotificationProperties(long cooldownMinutes) {
}
