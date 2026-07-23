package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * TV/monitor/game-console-style devices: mostly STANDBY, occasionally ACTIVE in usage windows
 * (yapılacak.md §15.5). Unlike the legacy stateless generator (which always draws a value from the
 * appliance's full active-power range, so e.g. a TV never actually reads a real standby wattage),
 * this model holds ACTIVE/STANDBY for a randomized window so the state doesn't flicker every tick
 * (§13.2), and draws STANDBY power from the catalog's real standby range instead.
 *
 * <p>Layered on top of the window: a low-probability {@code ABNORMAL_STANDBY} fault episode (§14)
 * that holds standby power abnormally high — still well below the active range, so it's a
 * "silent" anomaly today's safe-power-limit check can't catch, which is exactly the motivating
 * case for the {@code UNUSUAL_STANDBY_CONSUMPTION} trigger planned for a later phase. Fault status
 * only ever gets a fresh dice roll at a window boundary; while a window is open, its continuation
 * is a pure time comparison, preserving the "no flicker while the window is open" guarantee from
 * before faults existed.
 */
@Component
class StandbyDeviceBehaviorModel implements ApplianceBehaviorModel {

    private static final double MIN_ACTIVE_MINUTES = 15.0;
    private static final double MAX_ACTIVE_MINUTES = 90.0;
    private static final double MIN_STANDBY_MINUTES = 10.0;
    private static final double MAX_STANDBY_MINUTES = 60.0;
    private static final double MIN_ACTIVE_PROBABILITY = 0.05;
    private static final double MAX_ACTIVE_PROBABILITY = 0.95;

    /** Used only if a catalog item is somehow missing a standby range (all seeded STANDBY_DEVICE
     * items have one — this is just a safety net, not an expected path). */
    private static final BigDecimal DEFAULT_STANDBY_MIN_WATT = new BigDecimal("0.5");
    private static final BigDecimal DEFAULT_STANDBY_MAX_WATT = new BigDecimal("2");

    private static final String FAULT_CODE = "ABNORMAL_STANDBY";
    private static final double MIN_FAULT_MINUTES = 20.0;
    private static final double MAX_FAULT_MINUTES = 40.0;
    private static final BigDecimal ELEVATED_STANDBY_MIN_MULTIPLIER = new BigDecimal("3");
    private static final BigDecimal ELEVATED_STANDBY_MAX_MULTIPLIER = new BigDecimal("8");
    private static final BigDecimal ACTIVE_RANGE_HEADROOM = new BigDecimal("0.9");

    private final SimulationProperties properties;

    StandbyDeviceBehaviorModel(SimulationProperties properties) {
        this.properties = properties;
    }

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.STANDBY_DEVICE;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        Instant now = measuredAt.toInstant();
        boolean windowOpen = isWindowStillOpen(previousState, now);
        ApplianceRuntimeState windowState = windowOpen ? previousState : startNewWindow(config, measuredAt, random);

        ApplianceRuntimeState state = resolveFault(previousState, windowState, windowOpen, now, random);
        BigDecimal powerWatt = computePower(config, state, random);

        ApplianceRuntimeState nextState = new ApplianceRuntimeState(state.operatingState(), state.operatingMode(),
                state.stateStartedAt(), state.expectedStateEndAt(), state.activeFaultCode(), state.faultStartedAt(),
                state.faultExpectedEndAt(), now, state.sessionId());

        return new GeneratedReading(powerWatt, nextState);
    }

    private static boolean isWindowStillOpen(ApplianceRuntimeState previousState, Instant now) {
        return previousState != null && previousState.expectedStateEndAt() != null
                && now.isBefore(previousState.expectedStateEndAt());
    }

    private ApplianceRuntimeState startNewWindow(ApplianceConfig config, ZonedDateTime measuredAt,
                                                  RandomSource random) {
        boolean active = random.nextDouble() < activeProbability(config, measuredAt);
        double windowMinutes = active
                ? MIN_ACTIVE_MINUTES + random.nextDouble() * (MAX_ACTIVE_MINUTES - MIN_ACTIVE_MINUTES)
                : MIN_STANDBY_MINUTES + random.nextDouble() * (MAX_STANDBY_MINUTES - MIN_STANDBY_MINUTES);

        Instant now = measuredAt.toInstant();
        Instant windowEnd = now.plusSeconds(Math.round(windowMinutes * 60));
        ApplianceOperatingState operatingState = active ? ApplianceOperatingState.ACTIVE
                : ApplianceOperatingState.STANDBY;
        String mode = active ? "IN_USE" : "STANDBY";
        return new ApplianceRuntimeState(operatingState, mode, now, windowEnd, null, null, null, now, null);
    }

    /** Reuses the existing per-type diurnal demand curve ({@link ApplianceProfiles}) as a rough
     * proxy for "how likely is this device to be in active use right now" — a peak-hour multiplier
     * (~1.8 for TV evenings) maps to a high probability, an overnight trough (~0.1) to a low one.
     * Approximate on purpose; not calibrated against real usage data. */
    private double activeProbability(ApplianceConfig config, ZonedDateTime measuredAt) {
        ApplianceProfile profile = ApplianceProfiles.forType(config.type());
        double multiplier = TelemetryGenerator.diurnalMultiplier(profile, measuredAt, config.applianceId());
        double probability = multiplier / 2.0;
        return Math.max(MIN_ACTIVE_PROBABILITY, Math.min(MAX_ACTIVE_PROBABILITY, probability));
    }

    /** Faults only make sense while STANDBY (§14 rule 1/2). A fault already running when its
     * window is still open only ever ends via a time comparison — no fresh roll, preserving the
     * no-flicker guarantee. A fresh roll only happens at a window boundary, and only if no
     * still-valid fault is being carried over from the previous window. */
    private ApplianceRuntimeState resolveFault(ApplianceRuntimeState previousState, ApplianceRuntimeState windowState,
                                                boolean windowOpen, Instant now, RandomSource random) {
        if (windowState.operatingState() != ApplianceOperatingState.STANDBY) {
            return clearFault(windowState);
        }

        boolean priorFaultStillValid = previousState != null && previousState.activeFaultCode() != null
                && previousState.faultExpectedEndAt() != null && now.isBefore(previousState.faultExpectedEndAt());
        if (priorFaultStillValid) {
            return withFault(windowState, previousState.activeFaultCode(), previousState.faultStartedAt(),
                    previousState.faultExpectedEndAt());
        }

        if (!windowOpen && random.nextDouble() < properties.faultProbability()) {
            double durationMinutes = MIN_FAULT_MINUTES + random.nextDouble() * (MAX_FAULT_MINUTES - MIN_FAULT_MINUTES);
            Instant faultEnd = now.plusSeconds(Math.round(durationMinutes * 60));
            return withFault(windowState, FAULT_CODE, now, faultEnd);
        }

        return clearFault(windowState);
    }

    private BigDecimal computePower(ApplianceConfig config, ApplianceRuntimeState state, RandomSource random) {
        if (state.operatingState() == ApplianceOperatingState.ACTIVE) {
            return TelemetryGenerator.randomInRange(config.simulationMinWatt(), config.simulationMaxWatt(), random);
        }
        if (state.activeFaultCode() != null) {
            return TelemetryGenerator.randomInRange(elevatedStandbyMinWatt(config), elevatedStandbyMaxWatt(config),
                    random);
        }
        return TelemetryGenerator.randomInRange(standbyMinWatt(config), standbyMaxWatt(config), random);
    }

    private static ApplianceRuntimeState withFault(ApplianceRuntimeState state, String faultCode,
                                                    Instant faultStartedAt, Instant faultExpectedEndAt) {
        return new ApplianceRuntimeState(state.operatingState(), state.operatingMode(), state.stateStartedAt(),
                state.expectedStateEndAt(), faultCode, faultStartedAt, faultExpectedEndAt,
                state.previousMeasurementAt(), state.sessionId());
    }

    private static ApplianceRuntimeState clearFault(ApplianceRuntimeState state) {
        if (state.activeFaultCode() == null) {
            return state;
        }
        return new ApplianceRuntimeState(state.operatingState(), state.operatingMode(), state.stateStartedAt(),
                state.expectedStateEndAt(), null, null, null, state.previousMeasurementAt(), state.sessionId());
    }

    private static BigDecimal standbyMinWatt(ApplianceConfig config) {
        return config.standbyMinWatt() != null ? config.standbyMinWatt() : DEFAULT_STANDBY_MIN_WATT;
    }

    private static BigDecimal standbyMaxWatt(ApplianceConfig config) {
        return config.standbyMaxWatt() != null ? config.standbyMaxWatt() : DEFAULT_STANDBY_MAX_WATT;
    }

    private static BigDecimal elevatedStandbyMinWatt(ApplianceConfig config) {
        return standbyMaxWatt(config).multiply(ELEVATED_STANDBY_MIN_MULTIPLIER);
    }

    /** Bounded above by both a multiple of the normal standby ceiling and 90% of the active range's
     * floor, so an elevated-standby reading never drifts into "looks like it's actually active"
     * territory; if catalog ranges are unusually tight this can collapse the span, so a minimal
     * fallback span is guaranteed. */
    private static BigDecimal elevatedStandbyMaxWatt(ApplianceConfig config) {
        BigDecimal min = elevatedStandbyMinWatt(config);
        BigDecimal candidate = standbyMaxWatt(config).multiply(ELEVATED_STANDBY_MAX_MULTIPLIER);
        BigDecimal activeFloorCap = config.simulationMinWatt().multiply(ACTIVE_RANGE_HEADROOM);
        BigDecimal max = candidate.min(activeFloorCap);
        return max.compareTo(min) > 0 ? max : min.add(BigDecimal.ONE);
    }
}
