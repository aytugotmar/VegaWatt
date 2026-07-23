package com.vegawatt.core.telemetry.application;

import com.vegawatt.core.anomaly.domain.AnomalyEvaluationResult;
import com.vegawatt.core.anomaly.domain.StandbyAnomalyEvaluationResult;
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
import com.vegawatt.core.home.domain.ApplianceOperatingState;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.notification.domain.NotificationJob;
import com.vegawatt.core.notification.domain.NotificationJobRepository;
import com.vegawatt.core.telemetry.domain.ProcessedTelemetryEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryBillingRecorder {

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
    public void persist(UUID eventId, Home home, HomeUpdateOutcome homeOutcome, Appliance appliance,
                 AnomalyEvaluationResult anomalyResult, StandbyAnomalyEvaluationResult standbyResult,
                 BigDecimal observedPowerWatt, ApplianceOperatingState operatingState, boolean transitionedToResumed,
                 Instant occurredAt, Instant processedAt, int anomalyBreachThreshold) {
        processedTelemetryEventRepository.markProcessed(eventId, home.id(), appliance.id(), processedAt);

        String currentPeriod = BillingPeriodResolver.currentPeriod(occurredAt);
        BillingAccount billingAccount = billingAccountRepository.findByHomeIdAndBillingPeriod(home.id(), currentPeriod)
                .orElseGet(() -> BillingAccount.open(home.id(), currentPeriod, occurredAt));

        billingAccount.applyTelemetry(homeOutcome.energyIncrementKwh(), Money.of(homeOutcome.costIncrement()),
                occurredAt);

        recordEnergyQuotaEvent(home, homeOutcome.energyTransition(), billingAccount, occurredAt);
        recordBudgetQuotaEvent(home, homeOutcome.budgetTransition(), billingAccount, occurredAt);

        if (homeOutcome.budgetTransition().crossed100() && !billingAccount.penaltyActive()) {
            billingAccount.activatePenalty();
            operationalEventRepository.save(OperationalEvent.create(home.id(), null,
                    OperationalEventType.PENALTY_TARIFF_ACTIVATED, occurredAt, "penalty tariff activated"));
        }

        if (anomalyResult.transitionedToAnomalous()) {
            OperationalEvent saved = operationalEventRepository.save(OperationalEvent.create(home.id(),
                    appliance.id(), OperationalEventType.APPLIANCE_ANOMALY_DETECTED, occurredAt,
                    "appliance exceeded safe power limit for " + anomalyBreachThreshold + " consecutive readings"));
            createNotificationJob(saved, home.id(), AdvisoryTriggerType.ANOMALY, occurredAt);
        }
        if (anomalyResult.transitionedToRecovered()) {
            operationalEventRepository.save(OperationalEvent.create(home.id(), appliance.id(),
                    OperationalEventType.APPLIANCE_ANOMALY_RECOVERED, occurredAt,
                    "appliance power draw back to normal"));
        }

        if (standbyResult.transitionedToActive()) {
            String details = ("standby power exceeded threshold: operatingState=%s, observedPowerWatt=%s, "
                    + "expectedStandbyMaxWatt=%s, calculatedThresholdWatt=%s, consecutiveMeasurementCount=%d, "
                    + "detectedAt=%s").formatted(operatingState, observedPowerWatt, appliance.standbyMaxWatt(),
                    standbyResult.calculatedThresholdWatt(), standbyResult.standbyBreachCount(), occurredAt);
            OperationalEvent saved = operationalEventRepository.save(OperationalEvent.create(home.id(),
                    appliance.id(), OperationalEventType.APPLIANCE_STANDBY_ANOMALY_DETECTED, occurredAt, details));
            createNotificationJob(saved, home.id(), AdvisoryTriggerType.STANDBY_ANOMALY, occurredAt);
        }
        if (standbyResult.transitionedToRecovered()) {
            operationalEventRepository.save(OperationalEvent.create(home.id(), appliance.id(),
                    OperationalEventType.APPLIANCE_STANDBY_ANOMALY_RECOVERED, occurredAt,
                    "standby power draw back to normal after %d consecutive normal readings"
                            .formatted(standbyResult.standbyRecoveryCount())));
        }

        if (transitionedToResumed) {
            operationalEventRepository.save(OperationalEvent.create(home.id(), appliance.id(),
                    OperationalEventType.APPLIANCE_TELEMETRY_RESUMED, occurredAt,
                    "telemetry resumed after a period of staleness/offline silence"));
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
