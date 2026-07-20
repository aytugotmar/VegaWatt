package com.vegawatt.core.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vegawatt.web")
public record VegaWattWebProperties(String allowedOrigin) {
}
