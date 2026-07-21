package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TelemetryGenerator {

    private static final double SPIKE_PROBABILITY = 0.1;
    private static final BigDecimal SPIKE_MULTIPLIER = new BigDecimal("1.5");

    private TelemetryGenerator() {
    }

    /**
     * Shapes the reading around a per-appliance-type {@link ApplianceProfile}: a smooth diurnal
     * demand curve, plus (depending on the type) a thermostat duty cycle or a daily usage
     * session — see {@link ApplianceProfiles}. Spikes are deliberately exempt from all of this:
     * they exist purely to occasionally breach the safe limit so anomaly detection has something
     * to detect, regardless of what a "realistic" reading would be at that moment.
     */
    public static BigDecimal generatePowerWatt(ApplianceConfig config, RandomSource randomSource, ZonedDateTime time) {
        if (randomSource.nextDouble() < SPIKE_PROBABILITY) {
            return spikeReading(config, randomSource);
        }
        return normalReading(config, randomSource, time);
    }

    private static BigDecimal normalReading(ApplianceConfig config, RandomSource randomSource, ZonedDateTime time) {
        ApplianceProfile profile = ApplianceProfiles.forType(config.type());
        BigDecimal baseline = randomInRange(config.simulationMinWatt(), config.simulationMaxWatt(), randomSource);

        if (profile.dutyCycle() != null) {
            BigDecimal shaped = baseline.multiply(BigDecimal.valueOf(diurnalMultiplier(profile, time, config.applianceId())));
            boolean on = DiurnalCurve.dutyCycleOn(time, config.applianceId(), profile.dutyCycle());
            return on ? scale(shaped) : scale(shaped.multiply(BigDecimal.valueOf(profile.dutyCycle().standbyFraction())));
        }

        ApplianceProfile.Session session = activeSessionDefinition(profile, time);
        if (session != null) {
            boolean inSession = DiurnalCurve.inDailySession(time, config.applianceId(), session);
            return inSession ? scale(baseline) : scale(baseline.multiply(BigDecimal.valueOf(session.standbyFraction())));
        }

        return scale(baseline.multiply(BigDecimal.valueOf(diurnalMultiplier(profile, time, config.applianceId()))));
    }

    private static ApplianceProfile.Session activeSessionDefinition(ApplianceProfile profile, ZonedDateTime time) {
        if (DiurnalCurve.isWeekend(time) && profile.weekendSession() != null) {
            return profile.weekendSession();
        }
        return profile.weekdaySession();
    }

    private static double diurnalMultiplier(ApplianceProfile profile, ZonedDateTime time, UUID applianceId) {
        List<ApplianceProfile.Bump> bumps = new ArrayList<>(profile.bumps());
        if (DiurnalCurve.isWeekend(time)) {
            bumps.addAll(profile.weekendBumps());
        }
        return DiurnalCurve.diurnalMultiplier(DiurnalCurve.fractionalHour(time), bumps, applianceId);
    }

    private static BigDecimal spikeReading(ApplianceConfig config, RandomSource randomSource) {
        BigDecimal spikeCeiling = config.safePowerLimitWatt().multiply(SPIKE_MULTIPLIER);
        return randomInRange(config.safePowerLimitWatt(), spikeCeiling, randomSource);
    }

    private static BigDecimal randomInRange(BigDecimal min, BigDecimal max, RandomSource randomSource) {
        BigDecimal span = max.subtract(min);
        BigDecimal value = min.add(span.multiply(BigDecimal.valueOf(randomSource.nextDouble())));
        return scale(value);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
