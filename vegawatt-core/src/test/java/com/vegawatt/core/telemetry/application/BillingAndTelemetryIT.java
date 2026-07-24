package com.vegawatt.core.telemetry.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.vegawatt.core.anomaly.application.EvaluateApplianceAnomalyUseCase;
import com.vegawatt.core.anomaly.application.EvaluateStandbyConsumptionUseCase;
import com.vegawatt.core.billing.application.EvaluateHomeBillingUseCase;
import com.vegawatt.core.billing.domain.BillingAccount;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.config.AnomalyProperties;
import com.vegawatt.core.common.config.NotificationProperties;
import com.vegawatt.core.common.config.StandbyAnomalyProperties;
import com.vegawatt.core.common.events.OperationalEventRepository;
import com.vegawatt.core.common.time.BillingPeriodResolver;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceRepository;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.home.domain.TelemetryLiveStatePort;
import com.vegawatt.core.home.domain.TelemetryLiveStateUpdate;
import com.vegawatt.core.notification.domain.NotificationJobRepository;
import com.vegawatt.core.telemetry.domain.ProcessedTelemetryEventRepository;
import com.vegawatt.core.telemetry.domain.TelemetryReading;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A lightweight (pure-Mockito, no Testcontainers) end-to-end check that {@link ProcessTelemetryUseCase} and
 * {@link TelemetryBillingRecorder} still wire together correctly after the billing-period-rollover +
 * home/appliance compensation refactor.
 */
class BillingAndTelemetryIT {

    private HomeRepository homeRepository;
    private ApplianceRepository applianceRepository;
    private HomeLiveStatePort homeLiveStatePort;
    private TelemetryLiveStatePort telemetryLiveStatePort;
    private BillingAccountRepository billingAccountRepository;
    private OperationalEventRepository operationalEventRepository;
    private ProcessedTelemetryEventRepository processedTelemetryEventRepository;
    private NotificationJobRepository notificationJobRepository;
    private ClockProvider clockProvider;

    private ProcessTelemetryUseCase processTelemetryUseCase;
    private EvaluateHomeBillingUseCase evaluateHomeBillingUseCase;
    private EvaluateApplianceAnomalyUseCase evaluateApplianceAnomalyUseCase;
    private EvaluateStandbyConsumptionUseCase evaluateStandbyConsumptionUseCase;
    private TelemetryBillingRecorder telemetryBillingRecorder;

    private final UUID homeId = UUID.randomUUID();
    private final UUID applianceId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-07-22T10:00:00Z");
    private Home home;
    private Appliance appliance;
    private AtomicReference<HomeLiveState> currentHomeLiveState;

    @BeforeEach
    void setUp() {
        homeRepository = org.mockito.Mockito.mock(HomeRepository.class);
        applianceRepository = org.mockito.Mockito.mock(ApplianceRepository.class);
        homeLiveStatePort = org.mockito.Mockito.mock(HomeLiveStatePort.class);
        telemetryLiveStatePort = org.mockito.Mockito.mock(TelemetryLiveStatePort.class);
        billingAccountRepository = org.mockito.Mockito.mock(BillingAccountRepository.class);
        operationalEventRepository = org.mockito.Mockito.mock(OperationalEventRepository.class);
        processedTelemetryEventRepository = org.mockito.Mockito.mock(ProcessedTelemetryEventRepository.class);
        notificationJobRepository = org.mockito.Mockito.mock(NotificationJobRepository.class);
        clockProvider = () -> now;

        home = Home.reconstitute(homeId, "Villa", "owner@example.com", new BigDecimal("100"),
                new BigDecimal("500"), new BigDecimal("2.50"), new BigDecimal("5.00"), now, now, List.of());
        appliance = new Appliance(applianceId, homeId, "AC", "HVAC", new BigDecimal("2000"),
                new BigDecimal("500"), new BigDecimal("1500"), true, null, null, null, null, null);

        evaluateHomeBillingUseCase = new EvaluateHomeBillingUseCase(billingAccountRepository);
        AnomalyProperties anomalyProperties = new AnomalyProperties(3, 3, new BigDecimal("0.90"));
        evaluateApplianceAnomalyUseCase = new EvaluateApplianceAnomalyUseCase(anomalyProperties);
        evaluateStandbyConsumptionUseCase = new EvaluateStandbyConsumptionUseCase(
                new StandbyAnomalyProperties(new BigDecimal("3"), new BigDecimal("2"), 3, 3));

        telemetryBillingRecorder = new TelemetryBillingRecorder(billingAccountRepository, operationalEventRepository,
                processedTelemetryEventRepository, notificationJobRepository, new NotificationProperties(30));

        processTelemetryUseCase = new ProcessTelemetryUseCase(homeRepository, applianceRepository,
                homeLiveStatePort, telemetryLiveStatePort, evaluateHomeBillingUseCase,
                evaluateApplianceAnomalyUseCase, evaluateStandbyConsumptionUseCase, processedTelemetryEventRepository,
                telemetryBillingRecorder, clockProvider);

        lenient().when(homeRepository.findById(homeId)).thenReturn(Optional.of(home));
        lenient().when(applianceRepository.findById(applianceId)).thenReturn(Optional.of(appliance));

        String currentPeriod = BillingPeriodResolver.currentPeriod(now);
        lenient().when(billingAccountRepository.findByHomeIdAndBillingPeriod(homeId, currentPeriod))
                .thenReturn(Optional.of(BillingAccount.open(homeId, currentPeriod, now)));

        currentHomeLiveState = new AtomicReference<>(HomeLiveState.zero(homeId, "Villa", currentPeriod, now));

        lenient().when(telemetryLiveStatePort.update(any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    UnaryOperator<HomeLiveState> homeMutator = invocation.getArgument(2);
                    UnaryOperator<ApplianceLiveState> applianceMutator = invocation.getArgument(3);
                    HomeLiveState nextHome = homeMutator.apply(currentHomeLiveState.get());
                    currentHomeLiveState.set(nextHome);
                    ApplianceLiveState nextAppliance = applianceMutator.apply(null);
                    return new TelemetryLiveStateUpdate(nextHome, nextAppliance);
                });
    }

    @Test
    void execute_endToEndTelemetryIngestion_accumulatesEnergyAndPersistsBilling() {
        TelemetryReading reading = new TelemetryReading(UUID.randomUUID(), homeId, applianceId,
                new BigDecimal("1000"), null, null, 3600, now);

        processTelemetryUseCase.execute(reading);

        // 1000 Watts for 3600 seconds = 1.0 kWh
        assertThat(currentHomeLiveState.get().currentEnergyKwh()).isEqualByComparingTo("1.0");
        assertThat(currentHomeLiveState.get().currentCost()).isEqualTo(Money.of(new BigDecimal("2.50")));

        verify(billingAccountRepository).save(any(BillingAccount.class));
    }
}
