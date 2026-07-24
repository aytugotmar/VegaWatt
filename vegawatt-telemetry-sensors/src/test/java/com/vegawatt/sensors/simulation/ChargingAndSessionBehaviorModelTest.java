package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChargingAndSessionBehaviorModelTest {

    private static final ZonedDateTime DAY_START = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0)
            .withNano(0);
    private static final double WINDOW_START_HOUR = 8;
    private static final double WINDOW_END_HOUR = 20;
    private static final int MIN_DAILY_COUNT = 1;
    private static final int MAX_DAILY_COUNT = 2;
    private static final ChargingAndSessionBehaviorModel MODEL = new ChargingAndSessionBehaviorModel();

    private static ApplianceConfig robotVacuumConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "ROBOT_VACUUM", new BigDecimal("60"),
                new BigDecimal("20"), new BigDecimal("50"), "ROBOT_VACUUM", "CHARGING_AND_SESSION", null,
                new BigDecimal("1"));
    }

    /** The clock instant when this config's first (index-0) cleaning run is scheduled to start
     * today — derived from the same {@link DiurnalCurve#plannedSessionStartHour} the model itself
     * calls, so the test stays correct regardless of the random appliance id or the day it runs on. */
    private static ZonedDateTime firstPlannedCleaningStart(ApplianceConfig config) {
        double hour = DiurnalCurve.plannedSessionStartHour(DAY_START, config.applianceId(), 0, WINDOW_START_HOUR,
                WINDOW_END_HOUR, MIN_DAILY_COUNT, MAX_DAILY_COUNT);
        // +1s margin: Math.round can land a fraction of a second before the threshold, which would
        // make the model's own fractionalHour(measuredAt) >= plannedStart check fail by an epsilon.
        return DAY_START.plusSeconds(Math.round(hour * 3600) + 1);
    }

    @Test
    void progressesFromCleaningToChargingToDocked() {
        ApplianceConfig config = robotVacuumConfig();
        ZonedDateTime plannedStart = firstPlannedCleaningStart(config);

        var start = MODEL.generate(config, null, plannedStart, Duration.ofSeconds(5), () -> 0.0); // starts cleaning
        assertThat(start.nextState().operatingMode()).isEqualTo("CLEANING");
        assertThat(start.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);

        // Force straight past the cleaning stage's own end time.
        var afterCleaning = MODEL.generate(config, withPastEnd(start.nextState()),
                plannedStart.plusSeconds(5), Duration.ofSeconds(5), () -> 0.5);
        assertThat(afterCleaning.nextState().operatingMode()).isEqualTo("CHARGING");

        var afterCharging = MODEL.generate(config, withPastEnd(afterCleaning.nextState()),
                plannedStart.plusSeconds(10), Duration.ofSeconds(5), () -> 0.5);
        assertThat(afterCharging.nextState().operatingMode()).isEqualTo("DOCKED");
        assertThat(afterCharging.nextState().operatingState()).isEqualTo(ApplianceOperatingState.STANDBY);
        assertThat(afterCharging.powerWatt()).isLessThanOrEqualTo(config.standbyMaxWatt());
    }

    @Test
    void chargingPhasePowerTapersDownward() {
        ApplianceConfig config = robotVacuumConfig();

        ApplianceRuntimeState chargingStart = new ApplianceRuntimeState(ApplianceOperatingState.ACTIVE, "CHARGING",
                DAY_START.toInstant(), DAY_START.toInstant().plusSeconds(3600), null, null, null,
                DAY_START.toInstant(), null, null, null, 1, DAY_START.toLocalDate());

        var early = MODEL.generate(config, chargingStart, DAY_START.plusSeconds(60), Duration.ofSeconds(5), () -> 0.5);
        var late = MODEL.generate(config, chargingStart, DAY_START.plusSeconds(3000), Duration.ofSeconds(5),
                () -> 0.5);

        assertThat(late.powerWatt()).isLessThanOrEqualTo(early.powerWatt());
    }

    private static ApplianceRuntimeState withPastEnd(ApplianceRuntimeState state) {
        return new ApplianceRuntimeState(state.operatingState(), state.operatingMode(), state.stateStartedAt(),
                state.stateStartedAt(), state.activeFaultCode(), state.faultStartedAt(), state.faultExpectedEndAt(),
                state.previousMeasurementAt(), state.sessionId(), state.nextFaultEvaluationAt(),
                state.faultCooldownUntil(), state.sessionsToday(), state.sessionsCountedOnDate());
    }
}
