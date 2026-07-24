package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/**
 * Robot vacuums: a two-phase cycle of an active cleaning run (moderate, fairly steady power) then
 * a docked charging phase that tapers like {@link ChargingCurveBehaviorModel}, then idles at a
 * trickle until the next daily-capped cleaning run starts. Cleaning-run starts are scheduled via
 * {@link DiurnalCurve#plannedSessionStartHour} — a deterministic, per-day, spread-out plan — instead
 * of a flat per-tick probability, so runs land at realistic daytime hours instead of clumping right
 * after midnight.
 */
@Component
public class ChargingAndSessionBehaviorModel implements ApplianceBehaviorModel {

    private static final int MIN_DAILY_CLEANINGS = 1;
    private static final int MAX_DAILY_CLEANINGS = 2;
    private static final double WINDOW_START_HOUR = 8;
    private static final double WINDOW_END_HOUR = 20;
    private static final double MIN_CLEANING_SECONDS = 1200; // 20 min
    private static final double MAX_CLEANING_SECONDS = 2700; // 45 min
    private static final double MIN_CHARGE_SECONDS = 3600; // 60 min
    private static final double MAX_CHARGE_SECONDS = 5400; // 90 min

    @Override
    public ApplianceBehaviorProfile supportedProfile() {
        return ApplianceBehaviorProfile.CHARGING_AND_SESSION;
    }

    @Override
    public GeneratedReading generate(ApplianceConfig config, ApplianceRuntimeState previousState,
                                      ZonedDateTime measuredAt, Duration elapsed, RandomSource random) {
        Instant now = measuredAt.toInstant();
        String mode = previousState != null && previousState.operatingMode() != null
                ? previousState.operatingMode() : "DOCKED";
        Instant stageStartedAt = previousState != null ? previousState.stateStartedAt() : now;
        Instant expectedStageEndAt = previousState != null ? previousState.expectedStateEndAt() : null;
        int sessionsToday = previousState == null ? 0 : previousState.sessionsTodayAt(measuredAt.toLocalDate());

        BigDecimal cleaningPower = config.simulationMaxWatt() != null ? config.simulationMaxWatt() : config.safePowerLimitWatt();
        // The charge current isn't tied to the cleaning motor's peak draw — approximate the
        // charge-curve ceiling with the appliance's lower simulation bound instead.
        BigDecimal chargeCeiling = config.simulationMinWatt() != null ? config.simulationMinWatt() : cleaningPower;
        BigDecimal trickle = config.standbyMaxWatt() != null ? config.standbyMaxWatt() : BigDecimal.ZERO;

        String nextMode = mode;
        Instant nextStageStartedAt = stageStartedAt;
        Instant nextExpectedStageEndAt = expectedStageEndAt;

        if ("DOCKED".equals(mode)) {
            double plannedStart = DiurnalCurve.plannedSessionStartHour(measuredAt, config.applianceId(),
                    sessionsToday, WINDOW_START_HOUR, WINDOW_END_HOUR, MIN_DAILY_CLEANINGS, MAX_DAILY_CLEANINGS);
            boolean startsCleaning = !Double.isNaN(plannedStart)
                    && DiurnalCurve.fractionalHour(measuredAt) >= plannedStart;
            if (startsCleaning) {
                nextMode = "CLEANING";
                nextStageStartedAt = now;
                double seconds = MIN_CLEANING_SECONDS + random.nextDouble() * (MAX_CLEANING_SECONDS - MIN_CLEANING_SECONDS);
                nextExpectedStageEndAt = now.plusSeconds(Math.round(seconds));
                sessionsToday = sessionsToday + 1;
            }
        } else if ("CLEANING".equals(mode)) {
            if (expectedStageEndAt != null && !now.isBefore(expectedStageEndAt)) {
                nextMode = "CHARGING";
                nextStageStartedAt = now;
                double seconds = MIN_CHARGE_SECONDS + random.nextDouble() * (MAX_CHARGE_SECONDS - MIN_CHARGE_SECONDS);
                nextExpectedStageEndAt = now.plusSeconds(Math.round(seconds));
            }
        } else { // CHARGING
            if (expectedStageEndAt != null && !now.isBefore(expectedStageEndAt)) {
                nextMode = "DOCKED";
                nextStageStartedAt = now;
                nextExpectedStageEndAt = null;
            }
        }

        ApplianceOperatingState operatingState;
        BigDecimal powerWatt;
        switch (nextMode) {
            case "CLEANING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                double jitter = 0.9 + random.nextDouble() * 0.2;
                powerWatt = cleaningPower.multiply(BigDecimal.valueOf(jitter));
            }
            case "CHARGING" -> {
                operatingState = ApplianceOperatingState.ACTIVE;
                long totalSeconds = Duration.between(nextStageStartedAt, nextExpectedStageEndAt).getSeconds();
                long elapsedSeconds = Duration.between(nextStageStartedAt, now).getSeconds();
                double ratio = totalSeconds <= 0 ? 1.0 : Math.min(1.0, Math.max(0.0, (double) elapsedSeconds / totalSeconds));
                BigDecimal range = chargeCeiling.subtract(trickle);
                powerWatt = chargeCeiling.subtract(range.multiply(BigDecimal.valueOf(ratio)));
            }
            default -> { // DOCKED
                operatingState = ApplianceOperatingState.STANDBY;
                powerWatt = trickle;
            }
        }

        ApplianceRuntimeState nextRuntimeState = new ApplianceRuntimeState(
                operatingState, nextMode, nextStageStartedAt, nextExpectedStageEndAt, null, null, null, now, null,
                null, null, sessionsToday, measuredAt.toLocalDate()
        );

        return new GeneratedReading(powerWatt, nextRuntimeState);
    }
}
