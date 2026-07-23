package com.vegawatt.sensors.simulation;

/**
 * Sensors' own copy of the catalog behavior-profile vocabulary — this module is a separate
 * deployable from vegawatt-core and cannot import its {@code appliancecatalog.domain} enum, so the
 * value arrives over Kafka as a plain string ({@link com.vegawatt.sensors.registration.ApplianceConfig#behaviorProfile()})
 * and is parsed into this enum defensively (unknown/null values simply mean "no behavior model yet").
 */
public enum ApplianceBehaviorProfile {
    ALWAYS_ON_STABLE,
    ALWAYS_ON_VARIABLE,
    MANUAL_SWITCH,
    SCHEDULED_SWITCH,
    STANDBY_DEVICE,
    SHORT_SESSION,
    SHORT_HIGH_POWER,
    PROGRAM_CYCLE,
    THERMOSTATIC_CYCLE,
    THERMOSTATIC_SESSION,
    VARIABLE_LOAD,
    CHARGING_CURVE,
    CHARGING_AND_SESSION,
    FLOW_TRIGGERED
}
