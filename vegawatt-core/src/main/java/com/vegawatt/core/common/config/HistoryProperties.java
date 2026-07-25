package com.vegawatt.core.common.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vegawatt.history")
public record HistoryProperties(@Min(1) int maxRangeDays) {
}
