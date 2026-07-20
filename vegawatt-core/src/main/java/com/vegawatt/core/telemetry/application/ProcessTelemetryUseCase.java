package com.vegawatt.core.telemetry.application;

import com.vegawatt.core.anomaly.domain.AnomalyEvaluationResult;
import com.vegawatt.core.anomaly.domain.EvaluateApplianceAnomalyPolicy;
import com.vegawatt.core.billing.domain.BillingAccount;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.billing.domain.EvaluateQuotaPolicy;
import com.vegawatt.core.billing.domain.EvaluateTariffPolicy;
import com.vegawatt.core.billing.domain.QuotaTransition;
import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.ApplianceNotFoundException;
import com.vegawatt.core.home.domain.ApplianceRepository;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeNotFoundException;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.notification.application.NotificationOrchestrator;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.telemetry.domain.EnergyCalculator;
import com.vegawatt.core.telemetry.domain.InvalidTelemetryReadingException;
import com.vegawatt.core.telemetry.domain.ProcessedTelemetryEventRepository;
import com.vegawatt.core.telemetry.domain.TelemetryReading;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProcessTelemetryUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessTelemetryUseCase.class);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int PERCENTAGE_SCALE = 2;

    private final HomeRepository homeRepository;
    private final ApplianceRepository applianceRepository;
    private final HomeLiveStatePort homeLiveStatePort;
    private final ApplianceLiveStatePort applianceLiveStatePort;
    private final BillingAccountRepository billingAccountRepository;
    private final ProcessedTelemetryEventRepository processedTelemetryEventRepository;
    private final TelemetryBillingRecorder telemetryBillingRecorder;
    private final NotificationOrchestrator notificationOrchestrator;
    private final ClockProvider clockProvider;

    public ProcessTelemetryUseCase(HomeRepository homeRepository, ApplianceRepository applianceRepository,
                                    HomeLiveStatePort homeLiveStatePort, ApplianceLiveStatePort applianceLiveStatePort,
                                    BillingAccountRepository billingAccountRepository,
                                    ProcessedTelemetryEventRepository processedTelemetryEventRepository,
                                    TelemetryBillingRecorder telemetryBillingRecorder,
                                    NotificationOrchestrator notificationOrchestrator, ClockProvider clockProvider) {
        this.homeRepository = homeRepository;
        this.applianceRepository = applianceRepository;
        this.homeLiveStatePort = homeLiveStatePort;
        this.applianceLiveStatePort = applianceLiveStatePort;
        this.billingAccountRepository = billingAccountRepository;
        this.processedTelemetryEventRepository = processedTelemetryEventRepository;
        this.telemetryBillingRecorder = telemetryBillingRecorder;
        this.notificationOrchestrator = notificationOrchestrator;
        this.clockProvider = clockProvider;
    }

    public void execute(TelemetryReading reading) {
        if (processedTelemetryEventRepository.existsByEventId(reading.eventId())) {
            log.info("Skipping already processed telemetry event {}", reading.eventId());
            return;
        }

        Home home = homeRepository.findById(reading.homeId())
                .orElseThrow(() -> new HomeNotFoundException(reading.homeId()));
        Appliance appliance = applianceRepository.findById(reading.applianceId())
                .orElseThrow(() -> new ApplianceNotFoundException(reading.applianceId()));

        if (!appliance.homeId().equals(reading.homeId())) {
            throw new InvalidTelemetryReadingException(
                    "appliance " + appliance.id() + " does not belong to home " + reading.homeId());
        }
        if (!appliance.active()) {
            log.warn("Discarding telemetry event {} for inactive appliance {}", reading.eventId(), appliance.id());
            return;
        }

        Instant now = clockProvider.now();
        BigDecimal energyIncrementKwh = EnergyCalculator.incrementKwh(reading.powerWatt(),
                reading.measurementIntervalSeconds());

        HomeUpdateOutcome homeOutcome = updateHomeLiveState(home, energyIncrementKwh, now);
        AnomalyEvaluationResult anomalyResult = updateApplianceLiveState(reading, appliance, energyIncrementKwh, now);

        List<AdvisoryTriggerType> advisoryTriggers = telemetryBillingRecorder.persist(reading.eventId(), home,
                homeOutcome, appliance, anomalyResult, now);

        advisoryTriggers.forEach(triggerType -> notificationOrchestrator.triggerAdvisory(home.id(), triggerType));
    }

    private HomeUpdateOutcome updateHomeLiveState(Home home, BigDecimal energyIncrementKwh, Instant now) {
        AtomicReference<HomeUpdateOutcome> outcome = new AtomicReference<>();

        homeLiveStatePort.update(home.id(), current -> {
            HomeLiveState existing = current != null ? current : recoverHomeLiveState(home, now);
            Money costIncrement = EvaluateTariffPolicy.cost(energyIncrementKwh, existing.tariffState(),
                    home.baseTariffPerKwh(), home.penaltyTariffPerKwh());

            BigDecimal newEnergyKwh = existing.currentEnergyKwh().add(energyIncrementKwh);
            Money newCost = existing.currentCost().plus(costIncrement);
            BigDecimal newEnergyPercentage = percentage(newEnergyKwh, home.energyQuotaKwh());
            BigDecimal newBudgetPercentage = percentage(newCost.amount(), home.budgetQuotaTry());

            QuotaTransition energyTransition = EvaluateQuotaPolicy.evaluate(existing.energyQuotaPercentage(),
                    newEnergyPercentage);
            QuotaTransition budgetTransition = EvaluateQuotaPolicy.evaluate(existing.budgetQuotaPercentage(),
                    newBudgetPercentage);

            TariffState newTariffState = existing.tariffState() == TariffState.PENALTY || budgetTransition.crossed100()
                    ? TariffState.PENALTY
                    : TariffState.BASE;

            outcome.set(new HomeUpdateOutcome(energyIncrementKwh, costIncrement.amount(), energyTransition,
                    budgetTransition));

            return new HomeLiveState(home.id(), newEnergyKwh, newCost, newEnergyPercentage, newBudgetPercentage,
                    newTariffState, newTariffState == TariffState.PENALTY, now);
        });

        return outcome.get();
    }

    private HomeLiveState recoverHomeLiveState(Home home, Instant now) {
        return billingAccountRepository.findByHomeId(home.id())
                .map(billingAccount -> {
                    TariffState tariffState = billingAccount.penaltyActive() ? TariffState.PENALTY : TariffState.BASE;
                    BigDecimal energyPercentage = percentage(billingAccount.accumulatedEnergyKwh(),
                            home.energyQuotaKwh());
                    BigDecimal budgetPercentage = percentage(billingAccount.accumulatedCost().amount(),
                            home.budgetQuotaTry());
                    return new HomeLiveState(home.id(), billingAccount.accumulatedEnergyKwh(),
                            billingAccount.accumulatedCost(), energyPercentage, budgetPercentage, tariffState,
                            billingAccount.penaltyActive(), now);
                })
                .orElseGet(() -> HomeLiveState.zero(home.id(), now));
    }

    private AnomalyEvaluationResult updateApplianceLiveState(TelemetryReading reading, Appliance appliance,
                                                               BigDecimal energyIncrementKwh, Instant now) {
        AtomicReference<AnomalyEvaluationResult> outcome = new AtomicReference<>();

        applianceLiveStatePort.update(reading.homeId(), reading.applianceId(), current -> {
            ApplianceLiveState existing = current != null ? current
                    : ApplianceLiveState.zero(reading.homeId(), reading.applianceId(), now);

            AnomalyEvaluationResult result = EvaluateApplianceAnomalyPolicy.evaluate(
                    existing.consecutiveBreachCount(), existing.anomalous(), reading.powerWatt(),
                    appliance.safePowerLimitWatt());
            outcome.set(result);

            BigDecimal newAccumulatedEnergyKwh = existing.accumulatedEnergyKwh().add(energyIncrementKwh);
            return new ApplianceLiveState(reading.homeId(), reading.applianceId(), reading.powerWatt(),
                    newAccumulatedEnergyKwh, result.consecutiveBreachCount(), result.anomalous(), now);
        });

        return outcome.get();
    }

    private static BigDecimal percentage(BigDecimal value, BigDecimal quota) {
        return value.multiply(ONE_HUNDRED).divide(quota, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
    }
}
