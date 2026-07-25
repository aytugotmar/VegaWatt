package com.vegawatt.core.common.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vegawatt.gemini")
public record GeminiProperties(String apiKey, String model, int connectTimeoutMs, int readTimeoutMs,
                                @Min(1) int overallTimeoutMs) {
}
