package com.vegawatt.sensors.simulation;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Holds each appliance's current {@link ApplianceRuntimeState}, keyed by applianceId. Deliberately
 * separate from {@code HomeRegistry}: that class holds registration-driven data (what a
 * re-registration event overwrites), this one holds tick-driven simulation state — a
 * re-registration must never reset an appliance's in-progress operating window.
 */
@Component
public class ApplianceRuntimeStateStore {

    private final Map<UUID, ApplianceRuntimeState> states = new ConcurrentHashMap<>();

    public Optional<ApplianceRuntimeState> get(UUID applianceId) {
        return Optional.ofNullable(states.get(applianceId));
    }

    public void put(UUID applianceId, ApplianceRuntimeState state) {
        states.put(applianceId, state);
    }
}
