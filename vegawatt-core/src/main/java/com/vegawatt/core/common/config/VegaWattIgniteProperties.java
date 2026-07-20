package com.vegawatt.core.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vegawatt.ignite")
public record VegaWattIgniteProperties(String addresses) {
}
