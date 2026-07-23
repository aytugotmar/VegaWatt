package com.vegawatt.core.home.domain;

import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Updates a home's and its appliance's live state atomically, in a single Ignite transaction, so a
 * telemetry event can never leave one cache updated and the other not.
 */
public interface TelemetryLiveStatePort {

    TelemetryLiveStateUpdate update(UUID homeId, UUID applianceId, UnaryOperator<HomeLiveState> homeMutator,
                                     UnaryOperator<ApplianceLiveState> applianceMutator);

    void restore(UUID homeId, UUID applianceId, HomeLiveState previousHome, ApplianceLiveState previousAppliance);
}
