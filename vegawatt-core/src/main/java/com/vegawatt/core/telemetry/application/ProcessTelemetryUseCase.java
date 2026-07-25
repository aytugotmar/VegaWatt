package com.vegawatt.core.telemetry.application;

import com.vegawatt.core.anomaly.application.EvaluateApplianceAnomalyUseCase;
import com.vegawatt.core.anomaly.application.EvaluateStandbyConsumptionUseCase;
import com.vegawatt.core.anomaly.domain.AnomalyEvaluationResult;
import com.vegawatt.core.anomaly.domain.EvaluateStandbyConsumptionPolicy;
import com.vegawatt.core.anomaly.domain.StandbyAnomalyEvaluationResult;
import com.vegawatt.core.billing.application.EvaluateHomeBillingUseCase;
import com.vegawatt.core.billing.application.HomeBillingEvaluation;
import com.vegawatt.core.billing.application.HomeUpdateOutcome;
import com.vegawatt.core.common.ApplianceHealthStatus;
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
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class ProcessTelemetryUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessTelemetryUseCase.class);

    private final HomeRepository homeRepository;
    private final ApplianceRepository applianceRepository;
    @SuppressWarnings("unused")
    private final HomeLiveStatePort homeLiveStatePort;
    private final TelemetryLiveStatePort telemetryLiveStatePort;
    private final EvaluateHomeBillingUseCase evaluateHomeBillingUseCase;
    private final EvaluateApplianceAnomalyUseCase evaluateApplianceAnomalyUseCase;
    private final EvaluateStandbyConsumptionUseCase evaluateStandbyConsumptionUseCase;
    private final ProcessedTelemetryEventRepository processedTelemetryEventRepository;
    private final TelemetryBillingRecorder telemetryBillingRecorder;
    private final ClockProvider clockProvider;

    public ProcessTelemetryUseCase(HomeRepository homeRepository, ApplianceRepository applianceRepository,
                                    HomeLiveStatePort homeLiveStatePort, TelemetryLiveStatePort telemetryLiveStatePort,
                                    EvaluateHomeBillingUseCase evaluateHomeBillingUseCase,
                                    EvaluateApplianceAnomalyUseCase evaluateApplianceAnomalyUseCase,
                                    EvaluateStandbyConsumptionUseCase evaluateStandbyConsumptionUseCase,
                                    ProcessedTelemetryEventRepository processedTelemetryEventRepository,
                                    TelemetryBillingRecorder telemetryBillingRecorder, ClockProvider clockProvider) {
        this.homeRepository = homeRepository;
        this.applianceRepository = applianceRepository;
        this.homeLiveStatePort = homeLiveStatePort;
        this.telemetryLiveStatePort = telemetryLiveStatePort;
        this.evaluateHomeBillingUseCase = evaluateHomeBillingUseCase;
        this.evaluateApplianceAnomalyUseCase = evaluateApplianceAnomalyUseCase;
        this.evaluateStandbyConsumptionUseCase = evaluateStandbyConsumptionUseCase;
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

        Instant processedAt = clockProvider.now();
        Instant occurredAt = reading.occurredAt() != null ? reading.occurredAt() : processedAt;

        BigDecimal energyIncrementKwh = EnergyCalculator.incrementKwh(reading.powerWatt(),
                reading.measurementIntervalSeconds());

        // Home and appliance Ignite state are updated together in one transaction (see
        // IgniteTelemetryLiveStateAdapter) so a partial write is never observable; historical
        // logging in PostgreSQL follows and is explicitly guarded below so a logging failure is
        // never silent, and Ignite is compensated back to pre-event truth if it fails.
        AtomicReference<HomeLiveState> previousHomeRef = new AtomicReference<>();
        AtomicReference<ApplianceLiveState> previousApplianceRef = new AtomicReference<>();
        AtomicReference<HomeUpdateOutcome> homeOutcomeRef = new AtomicReference<>();
        AtomicReference<AnomalyEvaluationResult> anomalyResultRef = new AtomicReference<>();
        AtomicReference<StandbyAnomalyEvaluationResult> standbyResultRef = new AtomicReference<>();
        AtomicReference<Boolean> transitionedToResumedRef = new AtomicReference<>();

        telemetryLiveStatePort.update(home.id(), appliance.id(),
                current -> {
                    previousHomeRef.set(current);
                    HomeBillingEvaluation evaluation = evaluateHomeBillingUseCase.evaluate(home, current,
                            energyIncrementKwh, occurredAt, reading.eventId());
                    homeOutcomeRef.set(evaluation.outcome());
                    return evaluation.newState();
                },
                current -> {
                    previousApplianceRef.set(current);
                    ApplianceLiveState existing = current != null ? current
                            : ApplianceLiveState.zero(reading.homeId(), reading.applianceId(), appliance.name(),
                                    appliance.type(), appliance.safePowerLimitWatt(), occurredAt);

                    AnomalyEvaluationResult result = evaluateApplianceAnomalyUseCase.evaluate(
                            existing.consecutiveBreachCount(), existing.consecutiveNormalCount(), existing.anomalous(),
                            reading.powerWatt(), appliance.safePowerLimitWatt());
                    anomalyResultRef.set(result);

                    StandbyAnomalyEvaluationResult standbyResult = EvaluateStandbyConsumptionPolicy.isEligible(
                            reading.operatingState(), appliance.behaviorProfileSnapshot(), appliance.standbyMaxWatt())
                            ? evaluateStandbyConsumptionUseCase.evaluate(existing.standbyBreachCount(),
                                    existing.standbyRecoveryCount(), existing.standbyAnomalyActive(),
                                    reading.powerWatt(), appliance.standbyMaxWatt())
                            : StandbyAnomalyEvaluationResult.unchanged(existing.standbyBreachCount(),
                                    existing.standbyRecoveryCount(), existing.standbyAnomalyActive());
                    standbyResultRef.set(standbyResult);

                    // Any real telemetry event proves the appliance is reachable again, regardless
                    // of v1/v2 shape or operating state — unlike the standby rule, resumption isn't
                    // conditional on eligibility.
                    boolean transitionedToResumed = existing.telemetryHealthStatus() != ApplianceHealthStatus.NORMAL;
                    transitionedToResumedRef.set(transitionedToResumed);

                    BigDecimal newAccumulatedEnergyKwh = existing.accumulatedEnergyKwh().add(energyIncrementKwh);
                    return new ApplianceLiveState(reading.homeId(), reading.applianceId(), existing.applianceName(),
                            existing.applianceType(), existing.safePowerLimitWatt(), reading.powerWatt(),
                            reading.operatingState(), reading.operatingMode(), newAccumulatedEnergyKwh,
                            result.consecutiveBreachCount(), result.consecutiveNormalCount(), result.anomalous(),
                            standbyResult.standbyBreachCount(), standbyResult.standbyRecoveryCount(),
                            standbyResult.standbyAnomalyActive(), ApplianceHealthStatus.NORMAL, occurredAt,
                            reading.eventId());
                });

        HomeUpdateOutcome homeOutcome = homeOutcomeRef.get();
        AnomalyEvaluationResult anomalyResult = anomalyResultRef.get();
        StandbyAnomalyEvaluationResult standbyResult = standbyResultRef.get();
        boolean transitionedToResumed = transitionedToResumedRef.get();

        try {
            telemetryBillingRecorder.persist(reading.eventId(), home, homeOutcome, appliance, anomalyResult,
                    standbyResult, reading.powerWatt(), reading.operatingState(), transitionedToResumed, occurredAt,
                    processedAt, evaluateApplianceAnomalyUseCase.breachThreshold());
        } catch (DataIntegrityViolationException integrityEx) {
            // Ignite already applied this event's increment before this try block, so any failure
            // here — duplicate or not — leaves it ahead of the permanent ledger and must be
            // compensated regardless of which branch below is taken.
            compensateLiveState(home.id(), appliance.id(), reading.eventId(), previousHomeRef.get(),
                    previousApplianceRef.get());
            if (!isProcessedTelemetryEventDuplicate(integrityEx)) {
                // Anything other than the processed_telemetry_events primary-key collision — a
                // foreign-key violation, a not-null/check-constraint failure, a corrupt billing
                // row — is a real data problem, not a harmless double-delivery. Swallowing it here
                // would silently drop the telemetry permanently (offset still commits, no retry, no
                // DLT). Rethrow so it's handled exactly like any other unexpected failure below.
                log.error("Non-duplicate integrity violation persisting telemetry event {} (home={}, appliance={})",
                        reading.eventId(), home.id(), appliance.id(), integrityEx);
                throw integrityEx;
            }
            log.warn("Duplicate telemetry event {} detected during DB persist; restoring Ignite state and skipping",
                    reading.eventId());
        } catch (ObjectOptimisticLockingFailureException lockConflict) {
            // Distinct from the generic RuntimeException path below so this specific failure mode
            // is greppable/alertable on its own — not broken today (Kafka's own retry re-reads a
            // fresh billing_accounts row on the next attempt), but a rising rate of these would
            // signal real write contention on the same billing period that a generic error log
            // would bury alongside unrelated transient failures.
            log.warn("Optimistic lock conflict persisting billing account for telemetry event {} (home={}); " +
                            "compensating Ignite state, Kafka retry will re-read the current row",
                    reading.eventId(), home.id(), lockConflict);
            compensateLiveState(home.id(), appliance.id(), reading.eventId(), previousHomeRef.get(),
                    previousApplianceRef.get());
            throw lockConflict;
        } catch (RuntimeException e) {
            log.error("Failed to persist billing/event log for telemetry event {} (home={}, appliance={}) after " +
                            "Ignite update; compensating both home and appliance Ignite states back to pre-event truth",
                    reading.eventId(), home.id(), appliance.id(), e);
            compensateLiveState(home.id(), appliance.id(), reading.eventId(), previousHomeRef.get(),
                    previousApplianceRef.get());
            throw e;
        }
    }

    private static final String PROCESSED_TELEMETRY_EVENTS_CONSTRAINT_MARKER = "processed_telemetry_events";
    private static final String UNIQUE_VIOLATION_SQLSTATE = "23505";

    /**
     * True only for the specific, harmless case this catch block exists to handle: a second
     * delivery of the same telemetry event colliding with {@code processed_telemetry_events}'s
     * primary key. Any other {@link DataIntegrityViolationException} (a foreign-key violation, a
     * not-null/check-constraint failure, a corrupt billing row, ...) is a real data problem that
     * must not be swallowed as if it were a duplicate.
     */
    private static boolean isProcessedTelemetryEventDuplicate(DataIntegrityViolationException ex) {
        ConstraintViolationException constraintViolation = findConstraintViolation(ex);
        if (constraintViolation == null) {
            return false;
        }
        String sqlState = constraintViolation.getSQLState();
        String constraintName = constraintViolation.getConstraintName();
        return UNIQUE_VIOLATION_SQLSTATE.equals(sqlState) && constraintName != null
                && constraintName.contains(PROCESSED_TELEMETRY_EVENTS_CONSTRAINT_MARKER);
    }

    private static ConstraintViolationException findConstraintViolation(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                return constraintViolation;
            }
        }
        return null;
    }

    /**
     * A failed PostgreSQL persist leaves Ignite's live state ahead of the permanent ledger (the
     * energy/cost increment was already applied in Ignite but never durably recorded). Rebuilding
     * both the home's and appliance's live states back to their pre-event values in a single atomic
     * Ignite transaction undoes that drift cleanly — but only for whichever side's current cache
     * entry is still stamped with this event's ID; see {@link com.vegawatt.core.home.domain.TelemetryLiveStatePort#restore}
     * for why a plain unconditional restore would be unsafe under concurrent processing.
     */
    private void compensateLiveState(UUID homeId, UUID applianceId, UUID eventId, HomeLiveState previousHome,
                                     ApplianceLiveState previousAppliance) {
        try {
            telemetryLiveStatePort.restore(homeId, applianceId, eventId, previousHome, previousAppliance);
        } catch (RuntimeException compensationFailure) {
            log.error("Failed to compensate Ignite live state for home {} and appliance {}", homeId, applianceId,
                    compensationFailure);
        }
    }
}
