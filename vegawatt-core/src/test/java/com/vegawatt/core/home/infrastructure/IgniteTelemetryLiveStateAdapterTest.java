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
 * fix for a real bug where an unconditional restore could clobber a concurrently-committed later
 * event's live state. No real Ignite instance is used (none exists anywhere in this module's test
 * suite); {@link IgniteClient}/{@link ClientCache}/{@link ClientTransaction} are all plain
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
                new BigDecimal("5.00"), new BigDecimal("2.00"), TariffState.BASE, false, "2026-01", NOW, null);
    }

    private static ApplianceLiveState previousAppliance() {
        return new ApplianceLiveState(HOME_ID, APPLIANCE_ID, "Fridge", "REFRIGERATOR", new BigDecimal("200"),
                new BigDecimal("50"), null, null, BigDecimal.ZERO.setScale(9), 0, 0, false, 0, 0, false,
                com.vegawatt.core.common.ApplianceHealthStatus.NORMAL, NOW, null, 0L);
    }

    private static HomeLiveStateCacheValue homeCacheValueStampedWith(UUID eventId) {
        return new HomeLiveStateCacheValue("Test Ev", new BigDecimal("6.00"), new BigDecimal("11.00"),
                new BigDecimal("6.00"), new BigDecimal("2.20"), TariffState.BASE, false, "2026-01", NOW, eventId);
    }

    private static ApplianceLiveStateCacheValue applianceCacheValueStampedWith(UUID eventId) {
        return new ApplianceLiveStateCacheValue("Fridge", "REFRIGERATOR", new BigDecimal("200"),
                new BigDecimal("60"), null, null, BigDecimal.ZERO.setScale(9), 0, 0, false, 0, 0, false,
                com.vegawatt.core.common.ApplianceHealthStatus.NORMAL, NOW, eventId, 0L);
    }

    @Test
    void restoresBothStatesWhenCurrentCacheEntriesAreStillStampedWithTheEventBeingCompensated() {
        UUID eventId = UUID.randomUUID();
        when(homeCache.get(anyString())).thenReturn(homeCacheValueStampedWith(eventId));
        when(applianceCache.get(anyString())).thenReturn(applianceCacheValueStampedWith(eventId));

        adapter.restore(HOME_ID, APPLIANCE_ID, eventId, previousHome(), previousAppliance());

        verify(homeCache).put(anyString(), any(HomeLiveStateCacheValue.class));
        verify(applianceCache).put(anyString(), any(ApplianceLiveStateCacheValue.class));
    }

    @Test
    void skipsRestoringWhenCurrentCacheEntriesWereAlreadySupersededByALaterEvent() {
        UUID eventBeingCompensated = UUID.randomUUID();
        UUID laterEventThatAlreadyWon = UUID.randomUUID();
        when(homeCache.get(anyString())).thenReturn(homeCacheValueStampedWith(laterEventThatAlreadyWon));
        when(applianceCache.get(anyString())).thenReturn(applianceCacheValueStampedWith(laterEventThatAlreadyWon));

        adapter.restore(HOME_ID, APPLIANCE_ID, eventBeingCompensated, previousHome(), previousAppliance());

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

        adapter.restore(HOME_ID, APPLIANCE_ID, eventId, previousHome(), previousAppliance());

        verify(homeCache).put(anyString(), any(HomeLiveStateCacheValue.class));
        verify(applianceCache).put(anyString(), any(ApplianceLiveStateCacheValue.class));
    }
}
