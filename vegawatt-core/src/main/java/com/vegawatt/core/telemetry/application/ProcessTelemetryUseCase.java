package com.vegawatt.core.telemetry.application;

import com.vegawatt.core.anomaly.application.EvaluateApplianceAnomalyUseCase;
import com.vegawatt.core.anomaly.domain.AnomalyEvaluationResult;
import com.vegawatt.core.billing.application.EvaluateHomeBillingUseCase;
import com.vegawatt.core.billing.application.HomeBillingEvaluation;
import com.vegawatt.core.billing.application.HomeUpdateOutcome;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceNotFoundException;
import com.vegawatt.core.home.domain.ApplianceRepository;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeNotFoundException;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.home.domain.TelemetryLiveStatePort;
import com.vegawatt.core.telemetry.domain.EnergyCalculator;
import com.vegawatt.core.telemetry.domain.InvalidTelemetryReadingException;
import com.vegawatt.core.telemetry.domain.ProcessedTelemetryEventRepository;
import com.vegawatt.core.telemetry.domain.TelemetryReading;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
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
    private final TelemetryLiveStatePort telemetryLiveStatePort;
    private final EvaluateHomeBillingUseCase evaluateHomeBillingUseCase;
    private final EvaluateApplianceAnomalyUseCase evaluateApplianceAnomalyUseCase;
    private final ProcessedTelemetryEventRepository processedTelemetryEventRepository;
    private final TelemetryBillingRecorder telemetryBillingRecorder;
    private final ClockProvider clockProvider;

    public ProcessTelemetryUseCase(HomeRepository homeRepository, ApplianceRepository applianceRepository,
                                    HomeLiveStatePort homeLiveStatePort, TelemetryLiveStatePort telemetryLiveStatePort,
                                    EvaluateHomeBillingUseCase evaluateHomeBillingUseCase,
                                    EvaluateApplianceAnomalyUseCase evaluateApplianceAnomalyUseCase,
                                    ProcessedTelemetryEventRepository processedTelemetryEventRepository,
                                    TelemetryBillingRecorder telemetryBillingRecorder, ClockProvider clockProvider) {
        this.homeRepository = homeRepository;
        this.applianceRepository = applianceRepository;
        this.homeLiveStatePort = homeLiveStatePort;
        this.telemetryLiveStatePort = telemetryLiveStatePort;
        this.evaluateHomeBillingUseCase = evaluateHomeBillingUseCase;
        this.evaluateApplianceAnomalyUseCase = evaluateApplianceAnomalyUseCase;
        this.processedTelemetryEventRepository = processedTelemetryEventRepository;
        this.telemetryBillingRecorder = telemetryBillingRecorder;
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

        // Home and appliance Ignite state are updated together in one transaction (see
        // IgniteTelemetryLiveStateAdapter) so a partial write is never observable; historical
        // logging in PostgreSQL follows and is explicitly guarded below so a logging failure is
        // never silent, and Ignite is compensated back to Postgres's truth if it fails.
        AtomicReference<HomeLiveState> previousHomeRef = new AtomicReference<>();
        AtomicReference<ApplianceLiveState> previousApplianceRef = new AtomicReference<>();
        AtomicReference<HomeUpdateOutcome> homeOutcomeRef = new AtomicReference<>();
        AtomicReference<AnomalyEvaluationResult> anomalyResultRef = new AtomicReference<>();

        telemetryLiveStatePort.update(home.id(), appliance.id(),
                current -> {
                    previousHomeRef.set(current);
                    HomeBillingEvaluation evaluation = evaluateHomeBillingUseCase.evaluate(home, current,
                            energyIncrementKwh, now);
                    homeOutcomeRef.set(evaluation.outcome());
                    return evaluation.newState();
                },
                current -> {
                    previousApplianceRef.set(current);
                    ApplianceLiveState existing = current != null ? current
                            : ApplianceLiveState.zero(reading.homeId(), reading.applianceId(), appliance.name(),
                                    appliance.type(), appliance.safePowerLimitWatt(), now);

                    AnomalyEvaluationResult result = evaluateApplianceAnomalyUseCase.evaluate(
                            existing.consecutiveBreachCount(), existing.anomalous(), reading.powerWatt(),
                            appliance.safePowerLimitWatt());
                    anomalyResultRef.set(result);

                    BigDecimal newAccumulatedEnergyKwh = existing.accumulatedEnergyKwh().add(energyIncrementKwh);
                    return new ApplianceLiveState(reading.homeId(), reading.applianceId(), existing.applianceName(),
                            existing.applianceType(), existing.safePowerLimitWatt(), reading.powerWatt(),
                            newAccumulatedEnergyKwh, result.consecutiveBreachCount(), result.anomalous(), now);
                });

        HomeUpdateOutcome homeOutcome = homeOutcomeRef.get();
        AnomalyEvaluationResult anomalyResult = anomalyResultRef.get();

        try {
            telemetryBillingRecorder.persist(reading.eventId(), home, homeOutcome, appliance, anomalyResult, now,
                    evaluateApplianceAnomalyUseCase.breachThreshold());
        } catch (org.springframework.dao.DataIntegrityViolationException duplicateEx) {
            log.warn("Duplicate telemetry event {} detected during DB persist; restoring Ignite state and skipping",
                    reading.eventId());
            compensateLiveState(home.id(), appliance.id(), previousHomeRef.get(), previousApplianceRef.get());
        } catch (RuntimeException e) {
            log.error("Failed to persist billing/event log for telemetry event {} (home={}, appliance={}) after " +
                            "Ignite update; compensating both home and appliance Ignite states back to pre-event truth",
                    reading.eventId(), home.id(), appliance.id(), e);
            compensateLiveState(home.id(), appliance.id(), previousHomeRef.get(), previousApplianceRef.get());
            throw e;
        }
    }

    /**
     * A failed PostgreSQL persist leaves Ignite's live state ahead of the permanent ledger (the
     * energy/cost increment was already applied in Ignite but never durably recorded). Rebuilding
     * both the home's and appliance's live states back to their pre-event values in a single atomic
     * Ignite transaction undoes that drift cleanly.
     */
    private void compensateLiveState(UUID homeId, UUID applianceId, HomeLiveState previousHome,
                                     ApplianceLiveState previousAppliance) {
        try {
            telemetryLiveStatePort.restore(homeId, applianceId, previousHome, previousAppliance);
        } catch (RuntimeException compensationFailure) {
            log.error("Failed to compensate Ignite live state for home {} and appliance {}", homeId, applianceId,
                    compensationFailure);
        }
    }
}
