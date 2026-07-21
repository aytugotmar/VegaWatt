package com.vegawatt.core.home.infrastructure;

import com.vegawatt.core.common.Money;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
class IgniteHomeLiveStateAdapter implements HomeLiveStatePort {

    private static final String CACHE_NAME = "homeLiveState";

    private final IgniteClient igniteClient;
    private final ClientCache<String, HomeLiveStateCacheValue> cache;

    IgniteHomeLiveStateAdapter(IgniteClient igniteClient) {
        this.igniteClient = igniteClient;
        this.cache = igniteClient.getOrCreateCache(new ClientCacheConfiguration()
                .setName(CACHE_NAME)
                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL));
    }

    @Override
    public void initialize(HomeLiveState state) {
        cache.put(state.homeId().toString(), toCacheValue(state));
    }

    @Override
    public Optional<HomeLiveState> get(UUID homeId) {
        HomeLiveStateCacheValue value = cache.get(homeId.toString());
        return value == null ? Optional.empty() : Optional.of(toDomain(homeId, value));
    }

    @Override
    public List<HomeLiveState> getAll() {
        List<HomeLiveState> result = new ArrayList<>();
        try (var cursor = cache.query(new ScanQuery<String, HomeLiveStateCacheValue>())) {
            for (var entry : cursor) {
                result.add(toDomain(UUID.fromString(entry.getKey()), entry.getValue()));
            }
        }
        return result;
    }

    @Override
    public HomeLiveState update(UUID homeId, UnaryOperator<HomeLiveState> mutator) {
        String key = homeId.toString();
        try (ClientTransaction transaction = igniteClient.transactions().txStart()) {
            HomeLiveStateCacheValue existingValue = cache.get(key);
            HomeLiveState existing = existingValue == null ? null : toDomain(homeId, existingValue);
            HomeLiveState updated = mutator.apply(existing);
            cache.put(key, toCacheValue(updated));
            transaction.commit();
            return updated;
        }
    }

    private static HomeLiveStateCacheValue toCacheValue(HomeLiveState state) {
        return new HomeLiveStateCacheValue(state.homeName(), state.currentEnergyKwh(), state.currentCost().amount(),
                state.energyQuotaPercentage(), state.budgetQuotaPercentage(), state.tariffState(),
                state.penaltyActive(), state.lastUpdatedAt());
    }

    private static HomeLiveState toDomain(UUID homeId, HomeLiveStateCacheValue value) {
        return new HomeLiveState(homeId, value.getHomeName(), value.getCurrentEnergyKwh(),
                Money.of(value.getCurrentCost()), value.getEnergyQuotaPercentage(), value.getBudgetQuotaPercentage(),
                value.getTariffState(), value.isPenaltyActive(), value.getLastUpdatedAt());
    }
}
