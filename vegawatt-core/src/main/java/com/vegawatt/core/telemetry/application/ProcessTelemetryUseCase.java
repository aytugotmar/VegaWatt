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
import com.vegawatt.core.common.events.OperationalEvent;
import com.vegawatt.core.common.events.OperationalEventRepository;
import com.vegawatt.core.common.events.OperationalEventType;
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
import com.vegawatt.core.telemetry.domain.TelemetryReading;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
    private final OperationalEventRepository operationalEventRepository;
    private final NotificationOrchestrator notificationOrchestrator;
    private final ClockProvider clockProvider;

    public ProcessTelemetryUseCase(HomeRepository homeRepository, ApplianceRepository applianceRepository,
                                    HomeLiveStatePort homeLiveStatePort, ApplianceLiveStatePort applianceLiveStatePort,
                                    BillingAccountRepository billingAccountRepository,
                                    OperationalEventRepository operationalEventRepository,
                                    NotificationOrchestrator notificationOrchestrator, ClockProvider clockProvider) {
        this.homeRepository = homeRepository;
        this.applianceRepository = applianceRepository;
        this.homeLiveStatePort = homeLiveStatePort;
        this.applianceLiveStatePort = applianceLiveStatePort;
        this.billingAccountRepository = billingAccountRepository;
        this.operationalEventRepository = operationalEventRepository;
        this.notificationOrchestrator = notificationOrchestrator;
        this.clockProvider = clockProvider;
    }

    public void execute(TelemetryReading reading) {
        Home home = homeRepository.findById(reading.homeId())
                .orElseThrow(() -> new HomeNotFoundException(reading.homeId()));
        Appliance appliance = applianceRepository.findById(reading.applianceId())
                .orElseThrow(() -> new ApplianceNotFoundException(reading.applianceId()));

        Instant now = clockProvider.now();
        BigDecimal energyIncrementKwh = EnergyCalculator.incrementKwh(reading.powerWatt(),
                reading.measurementIntervalSeconds());

        HomeUpdateOutcome homeOutcome = updateHomeLiveState(home, energyIncrementKwh, now);
        AnomalyEvaluationResult anomalyResult = updateApplianceLiveState(reading, appliance, energyIncrementKwh, now);

        try {
            recordBillingAndOperationalEvents(home, homeOutcome, energyIncrementKwh, appliance, anomalyResult, now);
        } catch (RuntimeException e) {
            log.error("Failed to persist billing/operational updates for home {} appliance {}", home.id(),
                    appliance.id(), e);
        }
    }

    private HomeUpdateOutcome updateHomeLiveState(Home home, BigDecimal energyIncrementKwh, Instant now) {
        AtomicReference<HomeUpdateOutcome> outcome = new AtomicReference<>();

        homeLiveStatePort.update(home.id(), current -> {
            HomeLiveState existing = current != null ? current : HomeLiveState.zero(home.id(), now);
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

            outcome.set(new HomeUpdateOutcome(costIncrement.amount(), energyTransition, budgetTransition));

            return new HomeLiveState(home.id(), newEnergyKwh, newCost, newEnergyPercentage, newBudgetPercentage,
                    newTariffState, newTariffState == TariffState.PENALTY, now);
        });

        return outcome.get();
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

    private void recordBillingAndOperationalEvents(Home home, HomeUpdateOutcome homeOutcome,
                                                     BigDecimal energyIncrementKwh, Appliance appliance,
                                                     AnomalyEvaluationResult anomalyResult, Instant now) {
        BillingAccount billingAccount = billingAccountRepository.findByHomeId(home.id())
                .orElseThrow(() -> new HomeNotFoundException(home.id()));

        billingAccount.applyTelemetry(energyIncrementKwh, Money.of(homeOutcome.costIncrement()), now);

        if (homeOutcome.energyTransition().crossed80() && !billingAccount.energyQuota80Notified()) {
            billingAccount.markEnergyQuota80Notified();
            operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.QUOTA_80_REACHED, now, "energy quota reached 80%"));
            notificationOrchestrator.triggerAdvisory(home.id(), AdvisoryTriggerType.QUOTA_80);
        }
        if (homeOutcome.energyTransition().crossed100() && !billingAccount.energyQuota100Notified()) {
            billingAccount.markEnergyQuota100Notified();
            operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.QUOTA_100_REACHED, now, "energy quota reached 100%"));
            notificationOrchestrator.triggerAdvisory(home.id(), AdvisoryTriggerType.QUOTA_100);
        }
        if (homeOutcome.budgetTransition().crossed80() && !billingAccount.budgetQuota80Notified()) {
            billingAccount.markBudgetQuota80Notified();
            operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.QUOTA_80_REACHED, now, "budget quota reached 80%"));
            notificationOrchestrator.triggerAdvisory(home.id(), AdvisoryTriggerType.QUOTA_80);
        }
        if (homeOutcome.budgetTransition().crossed100() && !billingAccount.budgetQuota100Notified()) {
            billingAccount.markBudgetQuota100Notified();
            operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.QUOTA_100_REACHED, now, "budget quota reached 100%"));
            notificationOrchestrator.triggerAdvisory(home.id(), AdvisoryTriggerType.QUOTA_100);
        }
        if (homeOutcome.budgetTransition().crossed100() && !billingAccount.penaltyActive()) {
            billingAccount.activatePenalty();
            operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.PENALTY_TARIFF_ACTIVATED, now, "penalty tariff activated"));
        }

        if (anomalyResult.transitionedToAnomalous()) {
            operationalEventRepository.save(OperationalEvent.create(home.id(), appliance.id(),
                    OperationalEventType.APPLIANCE_ANOMALY_DETECTED, now,
                    "appliance exceeded safe power limit for 3 consecutive readings"));
            notificationOrchestrator.triggerAdvisory(home.id(), AdvisoryTriggerType.ANOMALY);
        }
        if (anomalyResult.transitionedToRecovered()) {
            operationalEventRepository.save(OperationalEvent.create(home.id(), appliance.id(),
                    OperationalEventType.APPLIANCE_ANOMALY_RECOVERED, now, "appliance power draw back to normal"));
        }

        billingAccountRepository.save(billingAccount);
    }

    private static BigDecimal percentage(BigDecimal value, BigDecimal quota) {
        return value.multiply(ONE_HUNDRED).divide(quota, PERCENTAGE_SCALE, RoundingMode.HALF_UP);
    }

    private record HomeUpdateOutcome(
            BigDecimal costIncrement,
            QuotaTransition energyTransition,
            QuotaTransition budgetTransition) {
    }
}
