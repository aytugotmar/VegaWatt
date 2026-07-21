package com.vegawatt.core.telemetry.application;

import com.vegawatt.core.anomaly.domain.AnomalyEvaluationResult;
import com.vegawatt.core.billing.application.HomeUpdateOutcome;
import com.vegawatt.core.billing.domain.BillingAccount;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.events.OperationalEvent;
import com.vegawatt.core.common.events.OperationalEventRepository;
import com.vegawatt.core.common.events.OperationalEventType;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeNotFoundException;
import com.vegawatt.core.notification.domain.AdvisoryTrigger;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.telemetry.domain.ProcessedTelemetryEventRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TelemetryBillingRecorder {

    private final BillingAccountRepository billingAccountRepository;
    private final OperationalEventRepository operationalEventRepository;
    private final ProcessedTelemetryEventRepository processedTelemetryEventRepository;

    TelemetryBillingRecorder(BillingAccountRepository billingAccountRepository,
                              OperationalEventRepository operationalEventRepository,
                              ProcessedTelemetryEventRepository processedTelemetryEventRepository) {
        this.billingAccountRepository = billingAccountRepository;
        this.operationalEventRepository = operationalEventRepository;
        this.processedTelemetryEventRepository = processedTelemetryEventRepository;
    }

    @Transactional
    List<AdvisoryTrigger> persist(UUID eventId, Home home, HomeUpdateOutcome homeOutcome, Appliance appliance,
                                   AnomalyEvaluationResult anomalyResult, Instant now, int anomalyBreachThreshold) {
        processedTelemetryEventRepository.markProcessed(eventId, home.id(), appliance.id(), now);

        BillingAccount billingAccount = billingAccountRepository.findByHomeId(home.id())
                .orElseThrow(() -> new HomeNotFoundException(home.id()));

        billingAccount.applyTelemetry(homeOutcome.energyIncrementKwh(), Money.of(homeOutcome.costIncrement()), now);

        List<AdvisoryTrigger> advisoryTriggers = new ArrayList<>();

        if (homeOutcome.energyTransition().crossed80() && !billingAccount.energyQuota80Notified()) {
            billingAccount.markEnergyQuota80Notified();
            OperationalEvent saved = operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.QUOTA_80_REACHED, now, "energy quota reached 80%"));
            advisoryTriggers.add(new AdvisoryTrigger(AdvisoryTriggerType.QUOTA_80, saved.id()));
        }
        if (homeOutcome.energyTransition().crossed100() && !billingAccount.energyQuota100Notified()) {
            billingAccount.markEnergyQuota100Notified();
            OperationalEvent saved = operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.QUOTA_100_REACHED, now, "energy quota reached 100%"));
            advisoryTriggers.add(new AdvisoryTrigger(AdvisoryTriggerType.QUOTA_100, saved.id()));
        }
        if (homeOutcome.budgetTransition().crossed80() && !billingAccount.budgetQuota80Notified()) {
            billingAccount.markBudgetQuota80Notified();
            OperationalEvent saved = operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.QUOTA_80_REACHED, now, "budget quota reached 80%"));
            advisoryTriggers.add(new AdvisoryTrigger(AdvisoryTriggerType.QUOTA_80, saved.id()));
        }
        if (homeOutcome.budgetTransition().crossed100() && !billingAccount.budgetQuota100Notified()) {
            billingAccount.markBudgetQuota100Notified();
            OperationalEvent saved = operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.QUOTA_100_REACHED, now, "budget quota reached 100%"));
            advisoryTriggers.add(new AdvisoryTrigger(AdvisoryTriggerType.QUOTA_100, saved.id()));
        }
        if (homeOutcome.budgetTransition().crossed100() && !billingAccount.penaltyActive()) {
            billingAccount.activatePenalty();
            operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.PENALTY_TARIFF_ACTIVATED, now, "penalty tariff activated"));
        }

        if (anomalyResult.transitionedToAnomalous()) {
            OperationalEvent saved = operationalEventRepository.save(OperationalEvent.create(home.id(),
                    appliance.id(), OperationalEventType.APPLIANCE_ANOMALY_DETECTED, now,
                    "appliance exceeded safe power limit for " + anomalyBreachThreshold + " consecutive readings"));
            advisoryTriggers.add(new AdvisoryTrigger(AdvisoryTriggerType.ANOMALY, saved.id()));
        }
        if (anomalyResult.transitionedToRecovered()) {
            operationalEventRepository.save(OperationalEvent.create(home.id(), appliance.id(),
                    OperationalEventType.APPLIANCE_ANOMALY_RECOVERED, now, "appliance power draw back to normal"));
        }

        billingAccountRepository.save(billingAccount);
        return advisoryTriggers;
    }
}
