package com.vegawatt.core.notification.domain;

public enum AdvisoryTriggerType {
    QUOTA_80,
    QUOTA_100,
    ANOMALY,
    STANDBY_ANOMALY,
    TELEMETRY_STALE,
    TELEMETRY_OFFLINE
}
