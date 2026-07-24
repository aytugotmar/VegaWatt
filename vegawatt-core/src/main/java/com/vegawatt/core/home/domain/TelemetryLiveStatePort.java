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

    /**
     * Restores a home's and appliance's live state to a pre-event snapshot after a failed
     * downstream persist — but only for whichever side's current cache entry is still stamped
     * with {@code eventId} (i.e. nobody else has legitimately overwritten it since). This
     * compare-and-swap guard prevents a delayed/duplicate compensation from clobbering a
     * different, later event's already-committed update to the same home/appliance.
     */
    void restore(UUID homeId, UUID applianceId, UUID eventId, HomeLiveState previousHome,
                 ApplianceLiveState previousAppliance);
}
