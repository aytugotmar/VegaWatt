package com.vegawatt.core.appliancecatalog.api;

import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.common.ApplianceTriggerType;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Derives the appliance-catalog API's {@code supportedTriggers} field: the full per-profile
 * trigger vocabulary from yapılacak.md §19, intersected with {@link #IMPLEMENTED_TRIGGERS} — the
 * set Core has actually built detection for. A profile absent from {@link #PROFILE_TRIGGERS} (only
 * {@code FLOW_TRIGGERED}, unused by any seeded catalog item today) resolves to an empty list rather
 * than requiring an exhaustive switch. Never queries Ignite/live-state: this describes static
 * catalog capability, not a specific appliance's current status.
 */
final class SupportedTriggerCatalog {

    private static final Set<ApplianceTriggerType> IMPLEMENTED_TRIGGERS = Set.of(
            ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED,
            ApplianceTriggerType.CONSECUTIVE_BREACH_THRESHOLD_REACHED,
            ApplianceTriggerType.ANOMALY_RECOVERED,
            ApplianceTriggerType.UNUSUAL_STANDBY_CONSUMPTION,
            ApplianceTriggerType.TELEMETRY_STALE,
            ApplianceTriggerType.TELEMETRY_OFFLINE,
            ApplianceTriggerType.TELEMETRY_RESUMED);

    private static final Map<ApplianceBehaviorProfile, List<ApplianceTriggerType>> PROFILE_TRIGGERS = Map.ofEntries(
            Map.entry(ApplianceBehaviorProfile.ALWAYS_ON_STABLE, List.of(
                    ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED, ApplianceTriggerType.TELEMETRY_STALE,
                    ApplianceTriggerType.TELEMETRY_OFFLINE, ApplianceTriggerType.TELEMETRY_RESUMED)),
            Map.entry(ApplianceBehaviorProfile.ALWAYS_ON_VARIABLE, List.of(
                    ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED, ApplianceTriggerType.TELEMETRY_STALE,
                    ApplianceTriggerType.TELEMETRY_OFFLINE)),
            Map.entry(ApplianceBehaviorProfile.MANUAL_SWITCH, List.of(
                    ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED, ApplianceTriggerType.SESSION_DURATION_EXCEEDED,
                    ApplianceTriggerType.UNEXPECTED_ACTIVE_PERIOD, ApplianceTriggerType.TELEMETRY_STALE)),
            Map.entry(ApplianceBehaviorProfile.SCHEDULED_SWITCH, List.of(
                    ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED, ApplianceTriggerType.EXPECTED_ACTIVITY_MISSING,
                    ApplianceTriggerType.UNEXPECTED_ACTIVE_PERIOD, ApplianceTriggerType.TELEMETRY_OFFLINE)),
            Map.entry(ApplianceBehaviorProfile.STANDBY_DEVICE, List.of(
                    ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED, ApplianceTriggerType.UNUSUAL_STANDBY_CONSUMPTION,
                    ApplianceTriggerType.SESSION_DURATION_EXCEEDED, ApplianceTriggerType.TELEMETRY_STALE)),
            Map.entry(ApplianceBehaviorProfile.SHORT_SESSION, List.of(
                    ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED, ApplianceTriggerType.SESSION_DURATION_EXCEEDED,
                    ApplianceTriggerType.TELEMETRY_STALE)),
            Map.entry(ApplianceBehaviorProfile.SHORT_HIGH_POWER, List.of(
                    ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED, ApplianceTriggerType.SESSION_DURATION_EXCEEDED,
                    ApplianceTriggerType.TELEMETRY_STALE)),
            Map.entry(ApplianceBehaviorProfile.PROGRAM_CYCLE, List.of(
                    ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED, ApplianceTriggerType.SESSION_DURATION_EXCEEDED,
                    ApplianceTriggerType.TELEMETRY_STALE, ApplianceTriggerType.ANOMALY_RECOVERED)),
            Map.entry(ApplianceBehaviorProfile.THERMOSTATIC_CYCLE, List.of(
                    ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED, ApplianceTriggerType.EXCESSIVE_DUTY_CYCLE,
                    ApplianceTriggerType.SESSION_DURATION_EXCEEDED, ApplianceTriggerType.TELEMETRY_STALE)),
            Map.entry(ApplianceBehaviorProfile.THERMOSTATIC_SESSION, List.of(
                    ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED, ApplianceTriggerType.EXCESSIVE_DUTY_CYCLE,
                    ApplianceTriggerType.SESSION_DURATION_EXCEEDED)),
            Map.entry(ApplianceBehaviorProfile.VARIABLE_LOAD, List.of(
                    ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED, ApplianceTriggerType.SESSION_DURATION_EXCEEDED,
                    ApplianceTriggerType.UNEXPECTED_ACTIVE_PERIOD, ApplianceTriggerType.TELEMETRY_STALE)),
            Map.entry(ApplianceBehaviorProfile.CHARGING_CURVE, List.of(
                    ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED, ApplianceTriggerType.SESSION_DURATION_EXCEEDED,
                    ApplianceTriggerType.UNUSUAL_STANDBY_CONSUMPTION)),
            Map.entry(ApplianceBehaviorProfile.CHARGING_AND_SESSION, List.of(
                    ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED, ApplianceTriggerType.SESSION_DURATION_EXCEEDED,
                    ApplianceTriggerType.EXPECTED_ACTIVITY_MISSING, ApplianceTriggerType.TELEMETRY_STALE)));

    private SupportedTriggerCatalog() {
    }

    static List<ApplianceTriggerType> resolve(ApplianceBehaviorProfile profile) {
        return PROFILE_TRIGGERS.getOrDefault(profile, List.of()).stream()
                .filter(IMPLEMENTED_TRIGGERS::contains)
                .toList();
    }
}
