package com.vegawatt.core.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vegawatt.jwt")
public record JwtProperties(String secret, int accessTokenTtlMinutes, int refreshTokenTtlDays) {
}
