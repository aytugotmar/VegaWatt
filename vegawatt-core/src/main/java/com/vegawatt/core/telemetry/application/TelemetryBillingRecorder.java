package com.vegawatt.core.telemetry.application;

import com.vegawatt.core.anomaly.domain.AnomalyEvaluationResult;
import com.vegawatt.core.billing.application.HomeUpdateOutcome;
import com.vegawatt.core.billing.domain.BillingAccount;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.billing.domain.QuotaTransition;
import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.events.OperationalEvent;
import com.vegawatt.core.common.events.OperationalEventRepository;
import com.vegawatt.core.common.events.OperationalEventType;
import com.vegawatt.core.common.time.BillingPeriodResolver;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.notification.domain.NotificationJob;
import com.vegawatt.core.notification.domain.NotificationJobRepository;
import com.vegawatt.core.telemetry.domain.ProcessedTelemetryEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class TelemetryBillingRecorder {

    private final BillingAccountRepository billingAccountRepository;
    private final OperationalEventRepository operationalEventRepository;
    private final ProcessedTelemetryEventRepository processedTelemetryEventRepository;
    private final NotificationJobRepository notificationJobRepository;

    TelemetryBillingRecorder(BillingAccountRepository billingAccountRepository,
                              OperationalEventRepository operationalEventRepository,
                              ProcessedTelemetryEventRepository processedTelemetryEventRepository,
                              NotificationJobRepository notificationJobRepository) {
        this.billingAccountRepository = billingAccountRepository;
        this.operationalEventRepository = operationalEventRepository;
        this.processedTelemetryEventRepository = processedTelemetryEventRepository;
        this.notificationJobRepository = notificationJobRepository;
    }

    @Transactional
    void persist(UUID eventId, Home home, HomeUpdateOutcome homeOutcome, Appliance appliance,
                 AnomalyEvaluationResult anomalyResult, Instant now, int anomalyBreachThreshold) {
        processedTelemetryEventRepository.markProcessed(eventId, home.id(), appliance.id(), now);

        String currentPeriod = BillingPeriodResolver.currentPeriod(now);
        BillingAccount billingAccount = billingAccountRepository.findByHomeIdAndBillingPeriod(home.id(), currentPeriod)
                .orElseGet(() -> BillingAccount.open(home.id(), currentPeriod, now));

        billingAccount.applyTelemetry(homeOutcome.energyIncrementKwh(), Money.of(homeOutcome.costIncrement()), now);

        recordEnergyQuotaEvent(home, homeOutcome.energyTransition(), billingAccount, now);
        recordBudgetQuotaEvent(home, homeOutcome.budgetTransition(), billingAccount, now);

        if (homeOutcome.budgetTransition().crossed100() && !billingAccount.penaltyActive()) {
            billingAccount.activatePenalty();
            operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.PENALTY_TARIFF_ACTIVATED, now, "penalty tariff activated"));
        }

        if (anomalyResult.transitionedToAnomalous()) {
            OperationalEvent saved = operationalEventRepository.save(OperationalEvent.create(home.id(),
                    appliance.id(), OperationalEventType.APPLIANCE_ANOMALY_DETECTED, now,
                    "appliance exceeded safe power limit for " + anomalyBreachThreshold + " consecutive readings"));
            createNotificationJob(saved, home.id(), AdvisoryTriggerType.ANOMALY, now);
        }
        if (anomalyResult.transitionedToRecovered()) {
            operationalEventRepository.save(OperationalEvent.create(home.id(), appliance.id(),
                    OperationalEventType.APPLIANCE_ANOMALY_RECOVERED, now, "appliance power draw back to normal"));
        }

        billingAccountRepository.save(billingAccount);
    }

    // 100% supersedes 80% within the same telemetry event, for the same quota dimension, so a
    // single measurement never fires both an 80% and a 100% advisory for the same thing.
    private void recordEnergyQuotaEvent(Home home, QuotaTransition transition, BillingAccount billingAccount,
                                         Instant now) {
        if (transition.crossed100() && !billingAccount.energyQuota100Notified()) {
            billingAccount.markEnergyQuota100Notified();
            billingAccount.markEnergyQuota80Notified();
            OperationalEvent saved = operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.ENERGY_QUOTA_100_REACHED, now, "energy quota reached 100%"));
            createNotificationJob(saved, home.id(), AdvisoryTriggerType.QUOTA_100, now);
        } else if (transition.crossed80() && !billingAccount.energyQuota80Notified()) {
            billingAccount.markEnergyQuota80Notified();
            OperationalEvent saved = operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.ENERGY_QUOTA_80_REACHED, now, "energy quota reached 80%"));
            createNotificationJob(saved, home.id(), AdvisoryTriggerType.QUOTA_80, now);
        }
    }

    private void recordBudgetQuotaEvent(Home home, QuotaTransition transition, BillingAccount billingAccount,
                                         Instant now) {
        if (transition.crossed100() && !billingAccount.budgetQuota100Notified()) {
            billingAccount.markBudgetQuota100Notified();
            billingAccount.markBudgetQuota80Notified();
            OperationalEvent saved = operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.BUDGET_QUOTA_100_REACHED, now, "budget quota reached 100%"));
            createNotificationJob(saved, home.id(), AdvisoryTriggerType.QUOTA_100, now);
        } else if (transition.crossed80() && !billingAccount.budgetQuota80Notified()) {
            billingAccount.markBudgetQuota80Notified();
            OperationalEvent saved = operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.BUDGET_QUOTA_80_REACHED, now, "budget quota reached 80%"));
            createNotificationJob(saved, home.id(), AdvisoryTriggerType.QUOTA_80, now);
        }
    }

    private void createNotificationJob(OperationalEvent triggerEvent, UUID homeId, AdvisoryTriggerType triggerType,
                                        Instant now) {
        notificationJobRepository.save(NotificationJob.create(triggerEvent.id(), homeId, triggerType, now));
    }
}
