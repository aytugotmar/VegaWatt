package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChargingCurveBehaviorModelTest {

    private static final ZonedDateTime NIGHT = ZonedDateTime.now().withHour(22).withMinute(0).withSecond(0);
    private static final ChargingCurveBehaviorModel MODEL = new ChargingCurveBehaviorModel();

    private static ApplianceConfig laptopConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "LAPTOP", new BigDecimal("100"),
                new BigDecimal("5"), new BigDecimal("90"), "LAPTOP", "CHARGING_CURVE", null, new BigDecimal("1"));
    }

    @Test
    void chargeCurveTrendsDownwardOverTheSession() {
        ApplianceConfig config = laptopConfig();

        var start = MODEL.generate(config, null, NIGHT, Duration.ofSeconds(5), () -> 0.0); // starts a session
        assertThat(start.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);
        BigDecimal startPower = start.powerWatt();

        // Sample a few points through the session (mid-session ticks must not roll a new session,
        // so keep the roll losing at 1.0 — a still-open session ignores it regardless).
        BigDecimal quarterPower = sampleAt(config, start.nextState(), NIGHT, start.nextState().expectedStateEndAt(), 0.25);
        BigDecimal halfPower = sampleAt(config, start.nextState(), NIGHT, start.nextState().expectedStateEndAt(), 0.5);
        BigDecimal threeQuartersPower = sampleAt(config, start.nextState(), NIGHT, start.nextState().expectedStateEndAt(), 0.75);

        assertThat(quarterPower).isLessThanOrEqualTo(startPower);
        assertThat(halfPower).isLessThanOrEqualTo(quarterPower);
        assertThat(threeQuartersPower).isLessThanOrEqualTo(halfPower);
    }

    @Test
    void neverStartsMoreSessionsThanTheDailyCap() {
        ZonedDateTime morning = ZonedDateTime.now().withHour(9).withMinute(0).withSecond(0);
        ApplianceConfig config = laptopConfig();
        ApplianceRuntimeState state = null;
        int sessionsStarted = 0;

        for (int tick = 0; tick < 8000; tick++) { // ~11h from 9am, stays within one calendar day
            ZonedDateTime measuredAt = morning.plusSeconds(5L * tick);
            var reading = MODEL.generate(config, state, measuredAt, Duration.ofSeconds(5), () -> 0.0);
            boolean sessionJustStarted = (state == null || state.operatingState() != ApplianceOperatingState.ACTIVE)
                    && reading.nextState().operatingState() == ApplianceOperatingState.ACTIVE;
            if (sessionJustStarted) {
                sessionsStarted++;
            }
            state = reading.nextState();
        }

        assertThat(sessionsStarted).isLessThanOrEqualTo(2); // LAPTOP's daily cap
    }

    private static BigDecimal sampleAt(ApplianceConfig config, ApplianceRuntimeState sessionState,
                                        ZonedDateTime sessionStart, java.time.Instant expectedEnd,
                                        double fractionThroughSession) {
        long totalSeconds = Duration.between(sessionStart.toInstant(), expectedEnd).getSeconds();
        ZonedDateTime measuredAt = sessionStart.plusSeconds(Math.round(totalSeconds * fractionThroughSession));
        var reading = MODEL.generate(config, sessionState, measuredAt, Duration.ofSeconds(5), () -> 1.0);
        return reading.powerWatt();
    }
}
