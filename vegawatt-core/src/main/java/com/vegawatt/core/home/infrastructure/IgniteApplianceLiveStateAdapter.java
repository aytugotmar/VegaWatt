package com.vegawatt.core.home.infrastructure;

import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientCacheConfiguration;
import org.apache.ignite.client.ClientTransaction;
import org.apache.ignite.client.IgniteClient;
import org.springframework.stereotype.Component;

@Component
class IgniteApplianceLiveStateAdapter implements ApplianceLiveStatePort {

    private static final String STATE_CACHE_NAME = "applianceLiveState";
    private static final String INDEX_CACHE_NAME = "applianceIdsByHome";

    private final IgniteClient igniteClient;
    private final ClientCache<String, ApplianceLiveStateCacheValue> stateCache;
    private final ClientCache<String, Set<String>> indexCache;

    IgniteApplianceLiveStateAdapter(IgniteClient igniteClient) {
        this.igniteClient = igniteClient;
        this.stateCache = igniteClient.getOrCreateCache(new ClientCacheConfiguration()
                .setName(STATE_CACHE_NAME)
                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL));
        this.indexCache = igniteClient.getOrCreateCache(new ClientCacheConfiguration()
                .setName(INDEX_CACHE_NAME)
                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL));
    }

    @Override
    public void initialize(ApplianceLiveState state) {
        String stateKey = compositeKey(state.homeId(), state.applianceId());
        String indexKey = state.homeId().toString();
        try (ClientTransaction transaction = igniteClient.transactions().txStart()) {
            stateCache.put(stateKey, toCacheValue(state));
            Set<String> applianceIds = indexCache.get(indexKey);
            applianceIds = applianceIds == null ? new HashSet<>() : new HashSet<>(applianceIds);
            applianceIds.add(state.applianceId().toString());
            indexCache.put(indexKey, applianceIds);
            transaction.commit();
        }
    }

    @Override
    public Optional<ApplianceLiveState> get(UUID homeId, UUID applianceId) {
        ApplianceLiveStateCacheValue value = stateCache.get(compositeKey(homeId, applianceId));
        return value == null ? Optional.empty() : Optional.of(toDomain(homeId, applianceId, value));
    }

    @Override
    public List<ApplianceLiveState> getByHomeId(UUID homeId) {
        Set<String> applianceIds = indexCache.get(homeId.toString());
        if (applianceIds == null || applianceIds.isEmpty()) {
            return List.of();
        }
        List<ApplianceLiveState> result = new ArrayList<>();
        for (String applianceIdText : applianceIds) {
            UUID applianceId = UUID.fromString(applianceIdText);
            ApplianceLiveStateCacheValue value = stateCache.get(compositeKey(homeId, applianceId));
            if (value != null) {
                result.add(toDomain(homeId, applianceId, value));
            }
        }
        return result;
    }

    @Override
    public List<ApplianceLiveState> getAll() {
        List<ApplianceLiveState> result = new ArrayList<>();
        try (var cursor = stateCache.query(new ScanQuery<String, ApplianceLiveStateCacheValue>())) {
            for (var entry : cursor) {
                String[] parts = entry.getKey().split(":", 2);
                result.add(toDomain(UUID.fromString(parts[0]), UUID.fromString(parts[1]), entry.getValue()));
            }
        }
        return result;
    }

    @Override
    public ApplianceLiveState update(UUID homeId, UUID applianceId, UnaryOperator<ApplianceLiveState> mutator) {
        String key = compositeKey(homeId, applianceId);
        try (ClientTransaction transaction = igniteClient.transactions().txStart()) {
            ApplianceLiveStateCacheValue existingValue = stateCache.get(key);
            ApplianceLiveState existing = existingValue == null ? null
                    : toDomain(homeId, applianceId, existingValue);
            ApplianceLiveState updated = mutator.apply(existing).withStateVersion(nextVersion(existing));
            stateCache.put(key, toCacheValue(updated));
            transaction.commit();
            return updated;
        }
    }

    private static long nextVersion(ApplianceLiveState existing) {
        return (existing == null ? 0L : existing.stateVersion()) + 1;
    }

    private static String compositeKey(UUID homeId, UUID applianceId) {
        return homeId + ":" + applianceId;
    }

    private static ApplianceLiveStateCacheValue toCacheValue(ApplianceLiveState state) {
        return new ApplianceLiveStateCacheValue(state.applianceName(), state.applianceType(),
                state.safePowerLimitWatt(), state.currentPowerWatt(), state.operatingState(), state.operatingMode(),
                state.accumulatedEnergyKwh(), state.consecutiveBreachCount(), state.consecutiveNormalCount(),
                state.anomalous(), state.standbyBreachCount(), state.standbyRecoveryCount(),
                state.standbyAnomalyActive(), state.telemetryHealthStatus(), state.lastUpdatedAt(),
                state.lastEventId(), state.lastProcessedSequence(), state.stateVersion());
    }

    private static ApplianceLiveState toDomain(UUID homeId, UUID applianceId, ApplianceLiveStateCacheValue value) {
        return new ApplianceLiveState(homeId, applianceId, value.getApplianceName(), value.getApplianceType(),
                value.getSafePowerLimitWatt(), value.getCurrentPowerWatt(), value.getOperatingState(),
                value.getOperatingMode(), value.getAccumulatedEnergyKwh(), value.getConsecutiveBreachCount(),
                value.getConsecutiveNormalCount(), value.isAnomalous(), value.getStandbyBreachCount(),
                value.getStandbyRecoveryCount(), value.isStandbyAnomalyActive(), value.getTelemetryHealthStatus(),
                value.getLastUpdatedAt(), value.getLastEventId(), value.getLastProcessedSequence(),
                value.getStateVersion());
    }
}
