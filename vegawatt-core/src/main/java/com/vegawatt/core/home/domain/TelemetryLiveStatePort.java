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
     * downstream persist — but only for whichever side's current cache entry still has the exact
     * {@code stateVersion} that {@code update()} stamped when this event's write happened (i.e.
     * nobody else — a concurrent telemetry event, health-scheduler sweep, or any other mutator —
     * has legitimately overwritten it since). This compare-and-swap guard prevents a delayed
     * compensation from clobbering a different, later mutation to the same home/appliance;
     * {@code eventId} is carried through purely for log correlation, not for the CAS itself (a
     * plain {@code lastEventId} match is insufficient — a later mutation by something other than
     * telemetry, e.g. {@code TelemetryHealthScheduler}, can leave {@code lastEventId} unchanged
     * while still being a newer state that must not be clobbered).
     */
    void restore(UUID homeId, UUID applianceId, UUID eventId, long expectedHomeVersion, long expectedApplianceVersion,
                 HomeLiveState previousHome, ApplianceLiveState previousAppliance);
}
