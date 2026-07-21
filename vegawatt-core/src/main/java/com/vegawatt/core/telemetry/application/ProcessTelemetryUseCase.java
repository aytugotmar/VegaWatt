package com.vegawatt.core.telemetry.application;

import com.vegawatt.core.anomaly.application.EvaluateApplianceAnomalyUseCase;
import com.vegawatt.core.anomaly.domain.AnomalyEvaluationResult;
import com.vegawatt.core.billing.application.EvaluateHomeBillingUseCase;
import com.vegawatt.core.billing.application.HomeBillingEvaluation;
import com.vegawatt.core.billing.application.HomeUpdateOutcome;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.ApplianceNotFoundException;
import com.vegawatt.core.home.domain.ApplianceRepository;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeNotFoundException;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.notification.application.NotificationOrchestrator;
import com.vegawatt.core.notification.domain.AdvisoryTrigger;
import com.vegawatt.core.telemetry.domain.EnergyCalculator;
import com.vegawatt.core.telemetry.domain.InvalidTelemetryReadingException;
import com.vegawatt.core.telemetry.domain.ProcessedTelemetryEventRepository;
import com.vegawatt.core.telemetry.domain.TelemetryReading;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProcessTelemetryUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessTelemetryUseCase.class);

    private final HomeRepository homeRepository;
    private final ApplianceRepository applianceRepository;
    private final HomeLiveStatePort homeLiveStatePort;
    private final ApplianceLiveStatePort applianceLiveStatePort;
    private final EvaluateHomeBillingUseCase evaluateHomeBillingUseCase;
    private final EvaluateApplianceAnomalyUseCase evaluateApplianceAnomalyUseCase;
    private final ProcessedTelemetryEventRepository processedTelemetryEventRepository;
    private final TelemetryBillingRecorder telemetryBillingRecorder;
    private final NotificationOrchestrator notificationOrchestrator;
    private final ClockProvider clockProvider;

    public ProcessTelemetryUseCase(HomeRepository homeRepository, ApplianceRepository applianceRepository,
                                    HomeLiveStatePort homeLiveStatePort, ApplianceLiveStatePort applianceLiveStatePort,
                                    EvaluateHomeBillingUseCase evaluateHomeBillingUseCase,
                                    EvaluateApplianceAnomalyUseCase evaluateApplianceAnomalyUseCase,
                                    ProcessedTelemetryEventRepository processedTelemetryEventRepository,
                                    TelemetryBillingRecorder telemetryBillingRecorder,
                                    NotificationOrchestrator notificationOrchestrator, ClockProvider clockProvider) {
        this.homeRepository = homeRepository;
        this.applianceRepository = applianceRepository;
        this.homeLiveStatePort = homeLiveStatePort;
        this.applianceLiveStatePort = applianceLiveStatePort;
        this.evaluateHomeBillingUseCase = evaluateHomeBillingUseCase;
        this.evaluateApplianceAnomalyUseCase = evaluateApplianceAnomalyUseCase;
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

        // Ignite (the volatile execution tier) is always updated first; historical logging in
        // Postgres follows and is explicitly guarded below so a logging failure is never silent.
        HomeUpdateOutcome homeOutcome = updateHomeLiveState(home, energyIncrementKwh, now);
        AnomalyEvaluationResult anomalyResult = updateApplianceLiveState(reading, appliance, energyIncrementKwh, now);

        List<AdvisoryTrigger> advisoryTriggers;
        try {
            advisoryTriggers = telemetryBillingRecorder.persist(reading.eventId(), home, homeOutcome, appliance,
                    anomalyResult, now, evaluateApplianceAnomalyUseCase.breachThreshold());
        } catch (RuntimeException e) {
            log.error("Failed to persist billing/event log for telemetry event {} (home={}, appliance={}) after " +
                            "Ignite update; rethrowing for dead-letter routing", reading.eventId(), home.id(),
                    appliance.id(), e);
            throw e;
        }

        advisoryTriggers.forEach(trigger -> notificationOrchestrator.triggerAdvisory(home.id(), trigger.type(),
                trigger.operationalEventId()));
    }

    private HomeUpdateOutcome updateHomeLiveState(Home home, BigDecimal energyIncrementKwh, Instant now) {
        AtomicReference<HomeUpdateOutcome> outcome = new AtomicReference<>();

        homeLiveStatePort.update(home.id(), current -> {
            HomeBillingEvaluation evaluation = evaluateHomeBillingUseCase.evaluate(home, current, energyIncrementKwh,
                    now);
            outcome.set(evaluation.outcome());
            return evaluation.newState();
        });

        return outcome.get();
    }

    private AnomalyEvaluationResult updateApplianceLiveState(TelemetryReading reading, Appliance appliance,
                                                               BigDecimal energyIncrementKwh, Instant now) {
        AtomicReference<AnomalyEvaluationResult> outcome = new AtomicReference<>();

        applianceLiveStatePort.update(reading.homeId(), reading.applianceId(), current -> {
            ApplianceLiveState existing = current != null ? current
                    : ApplianceLiveState.zero(reading.homeId(), reading.applianceId(), appliance.name(),
                            appliance.type(), appliance.safePowerLimitWatt(), now);

            AnomalyEvaluationResult result = evaluateApplianceAnomalyUseCase.evaluate(
                    existing.consecutiveBreachCount(), existing.anomalous(), reading.powerWatt(),
                    appliance.safePowerLimitWatt());
            outcome.set(result);

            BigDecimal newAccumulatedEnergyKwh = existing.accumulatedEnergyKwh().add(energyIncrementKwh);
            return new ApplianceLiveState(reading.homeId(), reading.applianceId(), existing.applianceName(),
                    existing.applianceType(), existing.safePowerLimitWatt(), reading.powerWatt(),
                    newAccumulatedEnergyKwh, result.consecutiveBreachCount(), result.anomalous(), now);
        });

        return outcome.get();
    }
}
