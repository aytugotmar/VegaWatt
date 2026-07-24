package com.vegawatt.core.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vegawatt.rate-limit")
public record RateLimitProperties(
        int registerPerMinute,
        int loginPerMinute,
        int refreshPerMinute,
        int insightPerMinute) {
}
