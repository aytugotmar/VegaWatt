package com.vegawatt.core.home.infrastructure;

import com.vegawatt.core.common.Money;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.TelemetryLiveStatePort;
import com.vegawatt.core.home.domain.TelemetryLiveStateUpdate;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientCacheConfiguration;
import org.apache.ignite.client.ClientTransaction;
import org.apache.ignite.client.IgniteClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Telemetry-processing write path: updates a home's and its appliance's Ignite live state in one
 * transaction, so a partial (home-only or appliance-only) write is never observable. Reuses the
 * same cache names as {@link IgniteHomeLiveStateAdapter}/{@link IgniteApplianceLiveStateAdapter},
 * which continue to own the read paths (get/getAll) and registration-time initialize().
 */
@Component
class IgniteTelemetryLiveStateAdapter implements TelemetryLiveStatePort {

    private static final Logger log = LoggerFactory.getLogger(IgniteTelemetryLiveStateAdapter.class);

    private static final String HOME_CACHE_NAME = "homeLiveState";
    private static final String APPLIANCE_CACHE_NAME = "applianceLiveState";

    private final IgniteClient igniteClient;
    private final ClientCache<String, HomeLiveStateCacheValue> homeCache;
    private final ClientCache<String, ApplianceLiveStateCacheValue> applianceCache;

    IgniteTelemetryLiveStateAdapter(IgniteClient igniteClient) {
        this.igniteClient = igniteClient;
        this.homeCache = igniteClient.getOrCreateCache(new ClientCacheConfiguration()
                .setName(HOME_CACHE_NAME)
                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL));
        this.applianceCache = igniteClient.getOrCreateCache(new ClientCacheConfiguration()
                .setName(APPLIANCE_CACHE_NAME)
                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL));
    }

    @Override
    public TelemetryLiveStateUpdate update(UUID homeId, UUID applianceId, UnaryOperator<HomeLiveState> homeMutator,
                                            UnaryOperator<ApplianceLiveState> applianceMutator) {
        String homeKey = homeId.toString();
        String applianceKey = homeId + ":" + applianceId;

        try (ClientTransaction transaction = igniteClient.transactions().txStart()) {
            HomeLiveStateCacheValue existingHomeValue = homeCache.get(homeKey);
            HomeLiveState existingHome = existingHomeValue == null ? null : toHomeDomain(homeId, existingHomeValue);
            HomeLiveState updatedHome = homeMutator.apply(existingHome)
                    .withStateVersion(nextVersion(existingHome == null ? 0L : existingHome.stateVersion()));
            homeCache.put(homeKey, toHomeCacheValue(updatedHome));

            ApplianceLiveStateCacheValue existingApplianceValue = applianceCache.get(applianceKey);
            ApplianceLiveState existingAppliance = existingApplianceValue == null ? null
                    : toApplianceDomain(homeId, applianceId, existingApplianceValue);
            ApplianceLiveState updatedAppliance = applianceMutator.apply(existingAppliance)
                    .withStateVersion(nextVersion(existingAppliance == null ? 0L : existingAppliance.stateVersion()));
            applianceCache.put(applianceKey, toApplianceCacheValue(updatedAppliance));

            transaction.commit();
            return new TelemetryLiveStateUpdate(updatedHome, updatedAppliance);
        }
    }

    private static long nextVersion(long existingVersion) {
        return existingVersion + 1;
    }

    @Override
    public void restore(UUID homeId, UUID applianceId, UUID eventId, long expectedHomeVersion,
                        long expectedApplianceVersion, HomeLiveState previousHome, ApplianceLiveState previousAppliance) {
        String homeKey = homeId.toString();
        String applianceKey = homeId + ":" + applianceId;

        try (ClientTransaction transaction = igniteClient.transactions().txStart()) {
            HomeLiveStateCacheValue currentHomeValue = homeCache.get(homeKey);
            if (currentHomeValue == null || currentHomeValue.getStateVersion() == expectedHomeVersion) {
                if (previousHome != null) {
                    homeCache.put(homeKey, toHomeCacheValue(previousHome));
                } else {
                    homeCache.remove(homeKey);
                }
            } else {
                log.warn("Skipping home Ignite compensation for event {} (home={}) — current state (version {}) was "
                        + "already superseded by a later write (expected version {})", eventId, homeId,
                        currentHomeValue.getStateVersion(), expectedHomeVersion);
            }

            ApplianceLiveStateCacheValue currentApplianceValue = applianceCache.get(applianceKey);
            if (currentApplianceValue == null || currentApplianceValue.getStateVersion() == expectedApplianceVersion) {
                if (previousAppliance != null) {
                    applianceCache.put(applianceKey, toApplianceCacheValue(previousAppliance));
                } else {
                    applianceCache.remove(applianceKey);
                }
            } else {
                log.warn("Skipping appliance Ignite compensation for event {} (appliance={}) — current state "
                        + "(version {}) was already superseded by a later write (expected version {})", eventId,
                        applianceId, currentApplianceValue.getStateVersion(), expectedApplianceVersion);
            }

            transaction.commit();
        }
    }

    private static HomeLiveStateCacheValue toHomeCacheValue(HomeLiveState state) {
        return new HomeLiveStateCacheValue(state.homeName(), state.currentEnergyKwh(), state.currentCost().amount(),
                state.energyQuotaPercentage(), state.budgetQuotaPercentage(), state.tariffState(),
                state.penaltyActive(), state.billingPeriod(), state.lastUpdatedAt(), state.lastEventId(),
                state.stateVersion());
    }

    private static HomeLiveState toHomeDomain(UUID homeId, HomeLiveStateCacheValue value) {
        return new HomeLiveState(homeId, value.getHomeName(), value.getCurrentEnergyKwh(),
                Money.of(value.getCurrentCost()), value.getEnergyQuotaPercentage(), value.getBudgetQuotaPercentage(),
                value.getTariffState(), value.isPenaltyActive(), value.getBillingPeriod(), value.getLastUpdatedAt(),
                value.getLastEventId(), value.getStateVersion());
    }

    private static ApplianceLiveStateCacheValue toApplianceCacheValue(ApplianceLiveState state) {
        return new ApplianceLiveStateCacheValue(state.applianceName(), state.applianceType(),
                state.safePowerLimitWatt(), state.currentPowerWatt(), state.operatingState(), state.operatingMode(),
                state.accumulatedEnergyKwh(), state.consecutiveBreachCount(), state.consecutiveNormalCount(),
                state.anomalous(), state.standbyBreachCount(), state.standbyRecoveryCount(),
                state.standbyAnomalyActive(), state.telemetryHealthStatus(), state.lastUpdatedAt(),
                state.lastEventId(), state.lastProcessedSequence(), state.stateVersion());
    }

    private static ApplianceLiveState toApplianceDomain(UUID homeId, UUID applianceId,
                                                          ApplianceLiveStateCacheValue value) {
        return new ApplianceLiveState(homeId, applianceId, value.getApplianceName(), value.getApplianceType(),
                value.getSafePowerLimitWatt(), value.getCurrentPowerWatt(), value.getOperatingState(),
                value.getOperatingMode(), value.getAccumulatedEnergyKwh(), value.getConsecutiveBreachCount(),
                value.getConsecutiveNormalCount(), value.isAnomalous(), value.getStandbyBreachCount(),
                value.getStandbyRecoveryCount(), value.isStandbyAnomalyActive(), value.getTelemetryHealthStatus(),
                value.getLastUpdatedAt(), value.getLastEventId(), value.getLastProcessedSequence(),
                value.getStateVersion());
    }
}
