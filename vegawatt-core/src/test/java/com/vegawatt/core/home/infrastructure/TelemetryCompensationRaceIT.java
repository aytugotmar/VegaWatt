package com.vegawatt.core.home.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.anomaly.application.EvaluateApplianceAnomalyUseCase;
import com.vegawatt.core.anomaly.application.EvaluateStandbyConsumptionUseCase;
import com.vegawatt.core.billing.application.EvaluateHomeBillingUseCase;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.common.ApplianceHealthStatus;
import com.vegawatt.core.common.config.AnomalyProperties;
import com.vegawatt.core.common.config.StandbyAnomalyProperties;
import com.vegawatt.core.common.time.BillingPeriodResolver;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceRepository;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.telemetry.application.ProcessTelemetryUseCase;
import com.vegawatt.core.telemetry.application.TelemetryBillingRecorder;
import com.vegawatt.core.telemetry.domain.ProcessedTelemetryEventRepository;
import com.vegawatt.core.telemetry.domain.TelemetryReading;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientCacheConfiguration;
import org.apache.ignite.client.ClientTransaction;
import org.apache.ignite.client.ClientTransactions;
import org.apache.ignite.client.IgniteClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * End-to-end reproduction of the race this session's stateVersion CAS fix left open: telemetry
 * event A writes Ignite, {@code TelemetryHealthScheduler} legitimately advances the appliance's
 * health status before A's own PostgreSQL persist fails, and A's compensation must still revert
 * A's own energy/sequence contribution without the health-scheduler write causing it to be skipped —
 * otherwise A's energy is permanently stranded in Ignite with no PostgreSQL row, and the
 * out-of-order guard's stale {@code lastProcessedSequence} silently drops every future Kafka retry
 * of the exact same event. Uses the *real* {@link IgniteApplianceLiveStateAdapter} and
 * {@link IgniteTelemetryLiveStateAdapter} (this test lives in their package specifically for that
 * access) against a plain in-memory map standing in for Ignite's cache, so the actual production CAS
 * logic is what's under test — not a re-implementation of it in the test.
 */
class TelemetryCompensationRaceIT {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID HOME_ID = UUID.randomUUID();
    private static final UUID APPLIANCE_ID = UUID.randomUUID();

    private final Map<String, ApplianceLiveStateCacheValue> applianceBackingMap = new ConcurrentHashMap<>();
    private final Map<String, HomeLiveStateCacheValue> homeBackingMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> indexBackingMap = new ConcurrentHashMap<>();

    private IgniteApplianceLiveStateAdapter applianceLiveStatePort;
    private IgniteTelemetryLiveStateAdapter telemetryLiveStatePort;
    private IgniteHomeLiveStateAdapter homeLiveStatePort;
    private HomeRepository homeRepository;
    private ApplianceRepository applianceRepository;
    private BillingAccountRepository billingAccountRepository;
    private ProcessedTelemetryEventRepository processedTelemetryEventRepository;
    private TelemetryBillingRecorder telemetryBillingRecorder;
    private ProcessTelemetryUseCase processTelemetryUseCase;

    private Home home;
    private Appliance appliance;

    @BeforeEach
    void setUp() {
        applianceLiveStatePort = new IgniteApplianceLiveStateAdapter(fakeIgniteClient());
        telemetryLiveStatePort = new IgniteTelemetryLiveStateAdapter(fakeIgniteClient());
        homeLiveStatePort = new IgniteHomeLiveStateAdapter(fakeIgniteClient());

        home = Home.reconstitute(HOME_ID, "Villa", "owner@example.com", new BigDecimal("500"),
                new BigDecimal("1000"), new BigDecimal("2.10"), new BigDecimal("3.50"), NOW, NOW, List.of());
        appliance = new Appliance(APPLIANCE_ID, HOME_ID, "Fridge", "REFRIGERATOR", new BigDecimal("200"),
                new BigDecimal("50"), new BigDecimal("150"), true, null, null, null, null, null);

        homeLiveStatePort.initialize(HomeLiveState.zero(HOME_ID, "Villa", BillingPeriodResolver.currentPeriod(NOW),
                NOW));
        applianceLiveStatePort.initialize(ApplianceLiveState.zero(HOME_ID, APPLIANCE_ID, "Fridge", "REFRIGERATOR",
                new BigDecimal("200"), NOW));

        homeRepository = mock(HomeRepository.class);
        applianceRepository = mock(ApplianceRepository.class);
        billingAccountRepository = mock(BillingAccountRepository.class);
        processedTelemetryEventRepository = mock(ProcessedTelemetryEventRepository.class);
        telemetryBillingRecorder = mock(TelemetryBillingRecorder.class);
        ClockProvider clockProvider = () -> NOW;

        lenient().when(homeRepository.findById(HOME_ID)).thenReturn(Optional.of(home));
        lenient().when(applianceRepository.findById(APPLIANCE_ID)).thenReturn(Optional.of(appliance));
        lenient().when(billingAccountRepository.findByHomeIdAndBillingPeriod(HOME_ID,
                BillingPeriodResolver.currentPeriod(NOW))).thenReturn(Optional.empty());

        EvaluateHomeBillingUseCase evaluateHomeBillingUseCase = new EvaluateHomeBillingUseCase(
                billingAccountRepository);
        EvaluateApplianceAnomalyUseCase evaluateApplianceAnomalyUseCase = new EvaluateApplianceAnomalyUseCase(
                new AnomalyProperties(3, 3, new BigDecimal("0.90")));
        EvaluateStandbyConsumptionUseCase evaluateStandbyConsumptionUseCase = new EvaluateStandbyConsumptionUseCase(
                new StandbyAnomalyProperties(new BigDecimal("3"), new BigDecimal("2"), 3, 3));

        processTelemetryUseCase = new ProcessTelemetryUseCase(homeRepository, applianceRepository,
                homeLiveStatePort, applianceLiveStatePort, telemetryLiveStatePort, evaluateHomeBillingUseCase,
                evaluateApplianceAnomalyUseCase, evaluateStandbyConsumptionUseCase, processedTelemetryEventRepository,
                telemetryBillingRecorder, clockProvider);
    }

    @Test
    void aRetryAfterCompensationIsNotDroppedAndHomeApplianceTotalsDoNotDiverge() {
        BigDecimal energyBeforeA = applianceLiveStatePort.get(HOME_ID, APPLIANCE_ID).orElseThrow()
                .accumulatedEnergyKwh();
        UUID eventIdA = UUID.randomUUID();
        TelemetryReading readingA = new TelemetryReading(eventIdA, HOME_ID, APPLIANCE_ID, 1L,
                new BigDecimal("1000"), null, null, 3600, NOW);

        // Simulates TelemetryHealthScheduler's sweep ticking in the window between event A's own
        // Ignite write and its PostgreSQL persist failing: a real, independent write to the SAME
        // shared appliance cache entry that advances telemetryHealthStatus without touching
        // lastEventId — exactly what the real scheduler's transition does.
        doAnswer(invocation -> {
            applianceLiveStatePort.update(HOME_ID, APPLIANCE_ID, existing -> new ApplianceLiveState(
                    existing.homeId(), existing.applianceId(), existing.applianceName(), existing.applianceType(),
                    existing.safePowerLimitWatt(), existing.currentPowerWatt(), existing.operatingState(),
                    existing.operatingMode(), existing.accumulatedEnergyKwh(), existing.consecutiveBreachCount(),
                    existing.consecutiveNormalCount(), existing.anomalous(), existing.standbyBreachCount(),
                    existing.standbyRecoveryCount(), existing.standbyAnomalyActive(), ApplianceHealthStatus.STALE,
                    existing.lastUpdatedAt(), existing.lastEventId(), existing.lastProcessedSequence(),
                    existing.stateVersion(), existing.catalogCode(), existing.catalogDisplayName(),
                    existing.catalogIconKey()));
            throw new RuntimeException("postgres unavailable");
        }).when(telemetryBillingRecorder).persist(any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), any(), anyInt());

        assertThatThrownBy(() -> processTelemetryUseCase.execute(readingA)).isInstanceOf(RuntimeException.class);

        ApplianceLiveState afterCompensation = applianceLiveStatePort.get(HOME_ID, APPLIANCE_ID).orElseThrow();
        assertThat(afterCompensation.telemetryHealthStatus())
                .as("the health scheduler's legitimately newer STALE status must survive A's compensation")
                .isEqualTo(ApplianceHealthStatus.STALE);
        assertThat(afterCompensation.accumulatedEnergyKwh())
                .as("A's own energy contribution must be reverted, not stranded")
                .isEqualByComparingTo(energyBeforeA);
        assertThat(afterCompensation.lastProcessedSequence())
                .as("the sequence high-water mark must revert too, or a Kafka retry of A is dropped forever")
                .isZero();

        // Kafka retries the exact same event; this time PostgreSQL is healthy again.
        doNothing().when(telemetryBillingRecorder).persist(any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), any(), anyInt());

        processTelemetryUseCase.execute(readingA);

        verify(telemetryBillingRecorder, times(2)).persist(any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), any(), anyInt());
        ApplianceLiveState afterRetry = applianceLiveStatePort.get(HOME_ID, APPLIANCE_ID).orElseThrow();
        HomeLiveState homeAfterRetry = homeLiveStatePort.get(HOME_ID).orElseThrow();
        assertThat(afterRetry.lastProcessedSequence()).isEqualTo(1L);
        // 1000W for 3600s = 1.0 kWh, counted exactly once — not twice, not zero.
        assertThat(afterRetry.accumulatedEnergyKwh()).isEqualByComparingTo(energyBeforeA.add(new BigDecimal("1.0")));
        assertThat(homeAfterRetry.currentEnergyKwh()).isEqualByComparingTo(afterRetry.accumulatedEnergyKwh());
    }

    private IgniteClient fakeIgniteClient() {
        IgniteClient client = mock(IgniteClient.class);
        ClientCache<String, ApplianceLiveStateCacheValue> applianceCache = mapBackedCache(applianceBackingMap);
        ClientCache<String, HomeLiveStateCacheValue> homeCache = mapBackedCache(homeBackingMap);
        ClientCache<String, Set<String>> indexCache = mapBackedCache(indexBackingMap);
        when(client.getOrCreateCache(any(ClientCacheConfiguration.class))).thenAnswer(invocation -> {
            ClientCacheConfiguration config = invocation.getArgument(0);
            return switch (config.getName()) {
                case "applianceLiveState" -> applianceCache;
                case "homeLiveState" -> homeCache;
                case "applianceIdsByHome" -> indexCache;
                default -> throw new IllegalArgumentException("Unexpected cache name: " + config.getName());
            };
        });
        ClientTransactions transactions = mock(ClientTransactions.class);
        when(client.transactions()).thenReturn(transactions);
        when(transactions.txStart()).thenAnswer(invocation -> mock(ClientTransaction.class));
        return client;
    }

    @SuppressWarnings("unchecked")
    private static <V> ClientCache<String, V> mapBackedCache(Map<String, V> backing) {
        ClientCache<String, V> cache = mock(ClientCache.class);
        when(cache.get(any())).thenAnswer(invocation -> backing.get((String) invocation.getArgument(0)));
        doAnswer(invocation -> {
            backing.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(cache).put(any(), any());
        doAnswer(invocation -> {
            backing.remove((String) invocation.getArgument(0));
            return null;
        }).when(cache).remove(any());
        when(cache.getAll(any())).thenAnswer(invocation -> {
            Set<String> keys = invocation.getArgument(0);
            Map<String, V> result = new HashMap<>();
            for (String key : keys) {
                V value = backing.get(key);
                if (value != null) {
                    result.put(key, value);
                }
            }
            return result;
        });
        return cache;
    }
}
