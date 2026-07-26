package com.vegawatt.core.home.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.ApplianceHealthStatus;
import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.HomeLiveState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientCacheConfiguration;
import org.apache.ignite.client.ClientTransaction;
import org.apache.ignite.client.ClientTransactions;
import org.apache.ignite.client.IgniteClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Verifies the two different compare-and-swap strategies in {@link IgniteTelemetryLiveStateAdapter#restore}:
 * home uses a whole-object {@code stateVersion} CAS (nothing outside telemetry processing ever
 * mutates home state), while appliance is gated on {@code lastEventId} and does a field-scoped merge
 * — {@code TelemetryHealthScheduler} is a second real writer that legitimately advances
 * {@code telemetryHealthStatus} without touching {@code lastEventId}, so a whole-object appliance CAS
 * would either wrongly skip the whole compensation (permanently stranding the failed event's energy/
 * sequence in Ignite with no matching PostgreSQL row) or wrongly clobber the scheduler's newer health
 * status. No real Ignite instance is used (none exists anywhere in this module's test suite);
 * {@link IgniteClient}/{@link ClientCache}/{@link ClientTransaction} are all plain interfaces and are
 * mocked directly.
 */
class IgniteTelemetryLiveStateAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID HOME_ID = UUID.randomUUID();
    private static final UUID APPLIANCE_ID = UUID.randomUUID();

    @SuppressWarnings("unchecked")
    private final ClientCache<String, HomeLiveStateCacheValue> homeCache = mock(ClientCache.class);
    @SuppressWarnings("unchecked")
    private final ClientCache<String, ApplianceLiveStateCacheValue> applianceCache = mock(ClientCache.class);
    private final IgniteClient igniteClient = mock(IgniteClient.class);

    private IgniteTelemetryLiveStateAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(igniteClient.getOrCreateCache(any(ClientCacheConfiguration.class)))
                .thenReturn((ClientCache) homeCache, (ClientCache) applianceCache);

        ClientTransactions transactions = mock(ClientTransactions.class);
        ClientTransaction transaction = mock(ClientTransaction.class);
        when(igniteClient.transactions()).thenReturn(transactions);
        when(transactions.txStart()).thenReturn(transaction);

        adapter = new IgniteTelemetryLiveStateAdapter(igniteClient);
    }

    private static HomeLiveState previousHome() {
        return new HomeLiveState(HOME_ID, "Test Ev", new BigDecimal("5.00"), Money.of(new BigDecimal("10.00")),
                new BigDecimal("5.00"), new BigDecimal("2.00"), TariffState.BASE, false, "2026-01", NOW, null, 0L);
    }

    private static ApplianceLiveState previousAppliance() {
        return new ApplianceLiveState(HOME_ID, APPLIANCE_ID, "Fridge", "REFRIGERATOR", new BigDecimal("200"),
                new BigDecimal("50"), null, null, BigDecimal.ZERO.setScale(9), 0, 0, false, 0, 0, false,
                ApplianceHealthStatus.NORMAL, NOW, null, 0L, 0L, null, null, null);
    }

    private static HomeLiveStateCacheValue homeCacheValueAtVersion(long stateVersion) {
        return new HomeLiveStateCacheValue("Test Ev", new BigDecimal("6.00"), new BigDecimal("11.00"),
                new BigDecimal("6.00"), new BigDecimal("2.20"), TariffState.BASE, false, "2026-01", NOW,
                UUID.randomUUID(), stateVersion);
    }

    private static ApplianceLiveStateCacheValue applianceCacheValueStampedWith(UUID eventId, long stateVersion,
                                                                                ApplianceHealthStatus healthStatus) {
        return new ApplianceLiveStateCacheValue("Fridge", "REFRIGERATOR", new BigDecimal("200"),
                new BigDecimal("60"), null, null, BigDecimal.ZERO.setScale(9), 0, 0, false, 0, 0, false,
                healthStatus, NOW, eventId, 3L, stateVersion, "REFRIGERATOR", "Buzdolabı", "refrigerator");
    }

    @Test
    void restoresHomeWhenCurrentVersionMatchesAndAppliancesLastEventIdMatches() {
        UUID eventId = UUID.randomUUID();
        long expectedHomeVersion = 3L;
        when(homeCache.get(anyString())).thenReturn(homeCacheValueAtVersion(expectedHomeVersion));
        when(applianceCache.get(anyString()))
                .thenReturn(applianceCacheValueStampedWith(eventId, 4L, ApplianceHealthStatus.NORMAL));

        adapter.restore(HOME_ID, APPLIANCE_ID, eventId, expectedHomeVersion, previousHome(), previousAppliance());

        verify(homeCache).put(anyString(), any(HomeLiveStateCacheValue.class));
        verify(applianceCache).put(anyString(), any(ApplianceLiveStateCacheValue.class));
    }

    @Test
    void skipsHomeRestoreWhenCurrentHomeVersionWasAlreadySupersededByALaterWrite() {
        UUID eventId = UUID.randomUUID();
        long expectedHomeVersion = 3L;
        // Some other write already advanced home past the version this compensation targets.
        when(homeCache.get(anyString())).thenReturn(homeCacheValueAtVersion(expectedHomeVersion + 1));
        when(applianceCache.get(anyString()))
                .thenReturn(applianceCacheValueStampedWith(eventId, 4L, ApplianceHealthStatus.NORMAL));

        adapter.restore(HOME_ID, APPLIANCE_ID, eventId, expectedHomeVersion, previousHome(), previousAppliance());

        verify(homeCache, never()).put(anyString(), any());
        verify(homeCache, never()).remove(anyString());
    }

    @Test
    void skipsApplianceRestoreWhenCurrentLastEventIdBelongsToADifferentTelemetryEvent() {
        UUID eventBeingCompensated = UUID.randomUUID();
        UUID laterEventThatAlreadyWon = UUID.randomUUID();
        when(homeCache.get(anyString())).thenReturn(homeCacheValueAtVersion(3L));
        when(applianceCache.get(anyString()))
                .thenReturn(applianceCacheValueStampedWith(laterEventThatAlreadyWon, 4L, ApplianceHealthStatus.NORMAL));

        adapter.restore(HOME_ID, APPLIANCE_ID, eventBeingCompensated, 3L, previousHome(), previousAppliance());

        verify(applianceCache, never()).put(anyString(), any());
        verify(applianceCache, never()).remove(anyString());
    }

    @Test
    void preservesTelemetryHealthStatusLegitimatelyAdvancedByHealthSchedulerWhileRevertingTelemetryOwnedFields() {
        // The scenario this test exists for: telemetry event A writes appliance state at version 5.
        // Before A's PostgreSQL persist fails, TelemetryHealthScheduler's sweep bumps the appliance
        // to STALE at version 6 — a real, legitimate write that never touches lastEventId. A's
        // compensation must still revert A's own energy/power/sequence contribution (otherwise it's
        // stranded in Ignite forever with no PostgreSQL row, and the out-of-order guard will wrongly
        // drop every future retry of the exact same event) while NOT clobbering the newer STALE
        // status back to NORMAL.
        UUID eventId = UUID.randomUUID();
        when(homeCache.get(anyString())).thenReturn(homeCacheValueAtVersion(3L));
        when(applianceCache.get(anyString()))
                .thenReturn(applianceCacheValueStampedWith(eventId, 6L, ApplianceHealthStatus.STALE));

        ApplianceLiveState previousAppliance = previousAppliance();
        adapter.restore(HOME_ID, APPLIANCE_ID, eventId, 3L, previousHome(), previousAppliance);

        ArgumentCaptor<ApplianceLiveStateCacheValue> captor =
                ArgumentCaptor.forClass(ApplianceLiveStateCacheValue.class);
        verify(applianceCache).put(anyString(), captor.capture());
        ApplianceLiveStateCacheValue written = captor.getValue();

        assertThat(written.getTelemetryHealthStatus())
                .as("the scheduler's legitimately newer health status must survive the compensation")
                .isEqualTo(ApplianceHealthStatus.STALE);
        assertThat(written.getAccumulatedEnergyKwh())
                .as("the failed event's own energy contribution must be reverted")
                .isEqualByComparingTo(previousAppliance.accumulatedEnergyKwh());
        assertThat(written.getLastProcessedSequence())
                .as("reverting the sequence high-water mark is what lets a Kafka retry of the same event "
                        + "through the out-of-order guard instead of it being silently dropped forever")
                .isEqualTo(previousAppliance.lastProcessedSequence());
        assertThat(written.getStateVersion())
                .as("the compensation write is itself a real mutation and must still advance the version")
                .isEqualTo(7L);
    }

    @Test
    void restoresWhenCurrentCacheEntriesAreAbsentSinceThereIsNothingToProtect() {
        UUID eventId = UUID.randomUUID();
        when(homeCache.get(anyString())).thenReturn(null);
        when(applianceCache.get(anyString())).thenReturn(null);

        adapter.restore(HOME_ID, APPLIANCE_ID, eventId, 3L, previousHome(), previousAppliance());

        verify(homeCache).put(anyString(), any(HomeLiveStateCacheValue.class));
        verify(applianceCache).put(anyString(), any(ApplianceLiveStateCacheValue.class));
    }
}
