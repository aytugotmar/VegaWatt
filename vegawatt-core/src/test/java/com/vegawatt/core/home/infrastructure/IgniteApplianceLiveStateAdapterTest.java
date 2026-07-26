package com.vegawatt.core.home.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.ApplianceHealthStatus;
import com.vegawatt.core.home.domain.ApplianceLiveState;
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

/**
 * First-ever test for this class — added while hardening {@code update()} against a no-op mutator
 * result skipping the stateVersion bump (needed so a concurrent telemetry compensation's CAS isn't
 * invalidated by a mutation that changed nothing) and against a null mutator result (a port-contract
 * gap: not reachable today since nothing deletes appliances yet, but cheap to guard).
 */
class IgniteApplianceLiveStateAdapterTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID HOME_ID = UUID.randomUUID();
    private static final UUID APPLIANCE_ID = UUID.randomUUID();

    @SuppressWarnings("unchecked")
    private final ClientCache<String, ApplianceLiveStateCacheValue> stateCache = mock(ClientCache.class);
    private final IgniteClient igniteClient = mock(IgniteClient.class);

    private IgniteApplianceLiveStateAdapter adapter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(igniteClient.getOrCreateCache(any(ClientCacheConfiguration.class))).thenReturn((ClientCache) stateCache);
        ClientTransactions transactions = mock(ClientTransactions.class);
        ClientTransaction transaction = mock(ClientTransaction.class);
        when(igniteClient.transactions()).thenReturn(transactions);
        when(transactions.txStart()).thenReturn(transaction);
        adapter = new IgniteApplianceLiveStateAdapter(igniteClient);
    }

    private static ApplianceLiveStateCacheValue existingValueAtVersion(long stateVersion) {
        return new ApplianceLiveStateCacheValue("Fridge", "REFRIGERATOR", new BigDecimal("200"),
                new BigDecimal("50"), null, null, BigDecimal.ZERO.setScale(9), 0, 0, false, 0, 0, false,
                ApplianceHealthStatus.NORMAL, NOW, null, 0L, stateVersion, null, null, null);
    }

    @Test
    void trueNoOpMutationDoesNotWriteOrBumpStateVersion() {
        when(stateCache.get(anyString())).thenReturn(existingValueAtVersion(5L));

        ApplianceLiveState result = adapter.update(HOME_ID, APPLIANCE_ID, existing -> existing);

        assertThat(result.stateVersion()).isEqualTo(5L);
        verify(stateCache, never()).put(anyString(), any());
    }

    @Test
    void realChangeStillBumpsStateVersionAndWrites() {
        when(stateCache.get(anyString())).thenReturn(existingValueAtVersion(5L));

        ApplianceLiveState result = adapter.update(HOME_ID, APPLIANCE_ID,
                existing -> new ApplianceLiveState(existing.homeId(), existing.applianceId(),
                        existing.applianceName(), existing.applianceType(), existing.safePowerLimitWatt(),
                        existing.currentPowerWatt(), existing.operatingState(), existing.operatingMode(),
                        existing.accumulatedEnergyKwh(), existing.consecutiveBreachCount(),
                        existing.consecutiveNormalCount(), existing.anomalous(), existing.standbyBreachCount(),
                        existing.standbyRecoveryCount(), existing.standbyAnomalyActive(),
                        ApplianceHealthStatus.STALE, existing.lastUpdatedAt(), existing.lastEventId(),
                        existing.lastProcessedSequence(), existing.stateVersion(), existing.catalogCode(),
                        existing.catalogDisplayName(), existing.catalogIconKey()));

        assertThat(result.stateVersion()).isEqualTo(6L);
        assertThat(result.telemetryHealthStatus()).isEqualTo(ApplianceHealthStatus.STALE);
        verify(stateCache).put(anyString(), any(ApplianceLiveStateCacheValue.class));
    }

    @Test
    void nullMutatorResultIsTreatedAsNoOpInsteadOfThrowing() {
        when(stateCache.get(anyString())).thenReturn(null);

        ApplianceLiveState result = adapter.update(HOME_ID, APPLIANCE_ID, existing -> null);

        assertThat(result).isNull();
        verify(stateCache, never()).put(anyString(), any());
    }
}
