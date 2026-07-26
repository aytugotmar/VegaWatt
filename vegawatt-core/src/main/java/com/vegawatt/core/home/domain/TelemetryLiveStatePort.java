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
     * downstream persist. The two sides use different compare-and-swap strategies because only one
     * of them has a second real writer:
     * <ul>
     *   <li><b>Home</b> — nothing outside telemetry processing ever mutates home state, so a
     *       whole-object {@code stateVersion} CAS against {@code expectedHomeVersion} (the version
     *       this event's own {@code update()} call stamped) is sufficient: if the current entry's
     *       version doesn't match, something legitimately newer has already committed and the
     *       restore is skipped entirely.</li>
     *   <li><b>Appliance</b> — {@code TelemetryHealthScheduler} is a second, independent writer that
     *       legitimately advances {@code telemetryHealthStatus} without touching {@code lastEventId}.
     *       A whole-object version CAS here would either wrongly skip the whole compensation (once
     *       the scheduler bumps the version) or, if keyed on {@code lastEventId} alone, wrongly
     *       clobber the scheduler's newer health status. So the appliance side gates on
     *       {@code lastEventId} matching this event, then reverts only the fields telemetry itself
     *       owns (energy, power, anomaly counters, sequence, event id) while carrying the
     *       <em>current</em> cache entry's {@code telemetryHealthStatus}/catalog cosmetics forward
     *       untouched — see {@code IgniteTelemetryLiveStateAdapter.restoreAppliance} for the exact
     *       field-level merge.</li>
     * </ul>
     */
    void restore(UUID homeId, UUID applianceId, UUID eventId, long expectedHomeVersion, HomeLiveState previousHome,
                 ApplianceLiveState previousAppliance);
}
