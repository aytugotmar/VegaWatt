package com.vegawatt.core.home.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.HomeLiveState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientCacheConfiguration;
import org.apache.ignite.client.ClientTransaction;
import org.apache.ignite.client.ClientTransactions;
import org.apache.ignite.client.IgniteClient;

/**
 * Verifies the compare-and-swap guard in {@link IgniteTelemetryLiveStateAdapter#restore} — the
 * fix for a real bug where an unconditional (or {@code lastEventId}-keyed) restore could clobber a
 * later write to the same home/appliance, whether that write came from a concurrently-committed
 * telemetry event or a completely different mutator (e.g. {@code TelemetryHealthScheduler}) that
 * never touches {@code lastEventId} at all. The CAS is keyed on {@code stateVersion} instead, which
 * every mutator advances. No real Ignite instance is used (none exists anywhere in this module's
 * test suite); {@link IgniteClient}/{@link ClientCache}/{@link ClientTransaction} are all plain
 * interfaces and are mocked directly.
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
                com.vegawatt.core.common.ApplianceHealthStatus.NORMAL, NOW, null, 0L, 0L);
    }

    private static HomeLiveStateCacheValue homeCacheValueAtVersion(long stateVersion) {
        return new HomeLiveStateCacheValue("Test Ev", new BigDecimal("6.00"), new BigDecimal("11.00"),
                new BigDecimal("6.00"), new BigDecimal("2.20"), TariffState.BASE, false, "2026-01", NOW,
                UUID.randomUUID(), stateVersion);
    }

    private static ApplianceLiveStateCacheValue applianceCacheValueAtVersion(long stateVersion) {
        return new ApplianceLiveStateCacheValue("Fridge", "REFRIGERATOR", new BigDecimal("200"),
                new BigDecimal("60"), null, null, BigDecimal.ZERO.setScale(9), 0, 0, false, 0, 0, false,
                com.vegawatt.core.common.ApplianceHealthStatus.NORMAL, NOW, UUID.randomUUID(), 0L, stateVersion);
    }

    @Test
    void restoresBothStatesWhenCurrentCacheEntriesAreStillAtTheVersionBeingCompensated() {
        UUID eventId = UUID.randomUUID();
        long expectedHomeVersion = 3L;
        long expectedApplianceVersion = 4L;
        when(homeCache.get(anyString())).thenReturn(homeCacheValueAtVersion(expectedHomeVersion));
        when(applianceCache.get(anyString())).thenReturn(applianceCacheValueAtVersion(expectedApplianceVersion));

        adapter.restore(HOME_ID, APPLIANCE_ID, eventId, expectedHomeVersion, expectedApplianceVersion, previousHome(),
                previousAppliance());

        verify(homeCache).put(anyString(), any(HomeLiveStateCacheValue.class));
        verify(applianceCache).put(anyString(), any(ApplianceLiveStateCacheValue.class));
    }

    @Test
    void skipsRestoringWhenCurrentCacheEntriesWereAlreadySupersededByALaterWrite() {
        UUID eventId = UUID.randomUUID();
        long expectedHomeVersion = 3L;
        long expectedApplianceVersion = 4L;
        // Some other mutator (a concurrent telemetry event, a health-scheduler sweep, ...) already
        // advanced both sides past the version this compensation is trying to roll back.
        when(homeCache.get(anyString())).thenReturn(homeCacheValueAtVersion(expectedHomeVersion + 1));
        when(applianceCache.get(anyString())).thenReturn(applianceCacheValueAtVersion(expectedApplianceVersion + 1));

        adapter.restore(HOME_ID, APPLIANCE_ID, eventId, expectedHomeVersion, expectedApplianceVersion, previousHome(),
                previousAppliance());

        verify(homeCache, never()).put(anyString(), any());
        verify(homeCache, never()).remove(anyString());
        verify(applianceCache, never()).put(anyString(), any());
        verify(applianceCache, never()).remove(anyString());
    }

    @Test
    void restoresWhenCurrentCacheEntriesAreAbsentSinceThereIsNothingToProtect() {
        UUID eventId = UUID.randomUUID();
        when(homeCache.get(anyString())).thenReturn(null);
        when(applianceCache.get(anyString())).thenReturn(null);

        adapter.restore(HOME_ID, APPLIANCE_ID, eventId, 3L, 4L, previousHome(), previousAppliance());

        verify(homeCache).put(anyString(), any(HomeLiveStateCacheValue.class));
        verify(applianceCache).put(anyString(), any(ApplianceLiveStateCacheValue.class));
    }
}
