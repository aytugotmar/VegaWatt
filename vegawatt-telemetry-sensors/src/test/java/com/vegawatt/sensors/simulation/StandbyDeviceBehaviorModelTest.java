package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class StandbyDeviceBehaviorModelTest {

    private static final ZonedDateTime NOON = ZonedDateTime.now().withHour(12).withMinute(0).withSecond(0);

    /** Below the model's clamped probability floor (0.05) — always decides ACTIVE regardless of
     * the actual diurnal-curve probability for the moment. */
    private static final double FORCE_ACTIVE = 0.0;

    /** Above the model's clamped probability ceiling (0.95) — always decides STANDBY. */
    private static final double FORCE_STANDBY = 0.99;

    private static ApplianceConfig tvConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "TV", new BigDecimal("180"),
                new BigDecimal("40"), new BigDecimal("150"), "TELEVISION", "STANDBY_DEVICE",
                new BigDecimal("0.5"), new BigDecimal("3"));
    }

    private static RandomSource sequenceOf(double... values) {
        AtomicInteger index = new AtomicInteger(0);
        return () -> values[index.getAndIncrement()];
    }

    private static StandbyDeviceBehaviorModel noFaultModel() {
        return new StandbyDeviceBehaviorModel(new SimulationProperties(5, 0.0, 1800, 1800));
    }

    private static StandbyDeviceBehaviorModel alwaysFaultModel() {
        return new StandbyDeviceBehaviorModel(new SimulationProperties(5, 1.0, 1800, 1800));
    }

    @Test
    void firstTickProducesActiveStateWithPowerInTheActiveRangeWhenForcedActive() {
        ApplianceConfig config = tvConfig();

        var reading = noFaultModel().generate(config, null, NOON, Duration.ofSeconds(5),
                sequenceOf(FORCE_ACTIVE, 0.5, 0.5));

        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);
        assertThat(reading.powerWatt()).isBetween(config.simulationMinWatt(), config.simulationMaxWatt());
    }

    @Test
    void standbyPowerStaysWithinTheCatalogStandbyRangeAndWellBelowActiveRange() {
        ApplianceConfig config = tvConfig();

        // Trailing value is the fault-start roll, which a faultProbability=0.0 model always rejects.
        var reading = noFaultModel().generate(config, null, NOON, Duration.ofSeconds(5),
                sequenceOf(FORCE_STANDBY, 0.5, 0.5, 1.0));

        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.STANDBY);
        assertThat(reading.nextState().activeFaultCode()).isNull();
        assertThat(reading.powerWatt()).isBetween(config.standbyMinWatt(), config.standbyMaxWatt());
        assertThat(reading.powerWatt()).isLessThan(config.simulationMinWatt());
    }

    @Test
    void stateDoesNotFlickerWhileTheCurrentWindowIsStillOpen() {
        ApplianceConfig config = tvConfig();
        StandbyDeviceBehaviorModel model = noFaultModel();

        // Establish an ACTIVE window with maximal duration (90 minutes).
        var first = model.generate(config, null, NOON, Duration.ofSeconds(5), sequenceOf(FORCE_ACTIVE, 1.0, 0.5));
        assertThat(first.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);

        // A few seconds later, well inside the 90-minute window: even a RandomSource that would
        // force STANDBY on a fresh decision must never be consulted, because the open window
        // short-circuits before any random call.
        var second = model.generate(config, first.nextState(), NOON.plusSeconds(5), Duration.ofSeconds(5),
                sequenceOf(FORCE_STANDBY, 0.0, 0.5));

        assertThat(second.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);
        assertThat(second.nextState().expectedStateEndAt()).isEqualTo(first.nextState().expectedStateEndAt());
    }

    @Test
    void reEvaluatesAfterTheWindowExpires() {
        ApplianceConfig config = tvConfig();
        StandbyDeviceBehaviorModel model = noFaultModel();

        // Establish an ACTIVE window with minimal duration (15 minutes).
        var first = model.generate(config, null, NOON, Duration.ofSeconds(5), sequenceOf(FORCE_ACTIVE, 0.0, 0.5));
        assertThat(first.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);

        // 16 minutes later — past the window's end — a fresh decision forcing STANDBY must apply.
        // Trailing value is the fault-start roll, always rejected by this faultProbability=0.0 model.
        var second = model.generate(config, first.nextState(), NOON.plusMinutes(16), Duration.ofSeconds(5),
                sequenceOf(FORCE_STANDBY, 0.5, 0.5, 1.0));

        assertThat(second.nextState().operatingState()).isEqualTo(ApplianceOperatingState.STANDBY);
    }

    @Test
    void forcedFaultOnANewStandbyWindowProducesElevatedButSubActivePower() {
        ApplianceConfig config = tvConfig();

        var reading = alwaysFaultModel().generate(config, null, NOON, Duration.ofSeconds(5),
                sequenceOf(FORCE_STANDBY, 0.5, 0.0, 0.5, 0.5));

        assertThat(reading.nextState().activeFaultCode()).isEqualTo("ABNORMAL_STANDBY");
        assertThat(reading.powerWatt()).isGreaterThan(config.standbyMaxWatt());
        assertThat(reading.powerWatt()).isLessThan(config.simulationMinWatt());
    }

    @Test
    void faultContinuesUnchangedWhileItsOwnWindowIsStillValid() {
        ApplianceConfig config = tvConfig();
        StandbyDeviceBehaviorModel model = alwaysFaultModel();

        var first = model.generate(config, null, NOON, Duration.ofSeconds(5),
                sequenceOf(FORCE_STANDBY, 0.5, 0.0, 0.5, 0.5));
        var second = model.generate(config, first.nextState(), NOON.plusMinutes(5), Duration.ofSeconds(5),
                sequenceOf(0.5));

        assertThat(second.nextState().activeFaultCode()).isEqualTo("ABNORMAL_STANDBY");
        assertThat(second.nextState().faultExpectedEndAt()).isEqualTo(first.nextState().faultExpectedEndAt());
        assertThat(second.powerWatt()).isGreaterThan(config.standbyMaxWatt());
    }

    @Test
    void faultClearsAndStandbyPowerReturnsToNormalAfterItExpiresButWindowIsStillOpen() {
        ApplianceConfig config = tvConfig();
        StandbyDeviceBehaviorModel model = alwaysFaultModel();

        // Window: 60 minutes (duration fraction 1.0). Fault: 20 minutes (duration fraction 0.0).
        var first = model.generate(config, null, NOON, Duration.ofSeconds(5),
                sequenceOf(FORCE_STANDBY, 1.0, 0.0, 0.0, 0.5));
        assertThat(first.nextState().expectedStateEndAt()).isEqualTo(NOON.plusMinutes(60).toInstant());
        assertThat(first.nextState().faultExpectedEndAt()).isEqualTo(NOON.plusMinutes(20).toInstant());

        // 25 minutes later: still inside the 60-minute window, but past the 20-minute fault.
        var second = model.generate(config, first.nextState(), NOON.plusMinutes(25), Duration.ofSeconds(5),
                sequenceOf(0.5));

        assertThat(second.nextState().operatingState()).isEqualTo(ApplianceOperatingState.STANDBY);
        assertThat(second.nextState().activeFaultCode()).isNull();
        assertThat(second.powerWatt()).isBetween(config.standbyMinWatt(), config.standbyMaxWatt());
    }

    @Test
    void transitioningToActiveClearsAnyInProgressFault() {
        ApplianceConfig config = tvConfig();
        StandbyDeviceBehaviorModel model = alwaysFaultModel();

        // STANDBY window: 10 minutes. Fault: 40 minutes (would still be "valid" past the window).
        var first = model.generate(config, null, NOON, Duration.ofSeconds(5),
                sequenceOf(FORCE_STANDBY, 0.0, 0.0, 1.0, 0.5));
        assertThat(first.nextState().activeFaultCode()).isEqualTo("ABNORMAL_STANDBY");

        // 11 minutes later: the STANDBY window has ended, and this fresh decision forces ACTIVE.
        var second = model.generate(config, first.nextState(), NOON.plusMinutes(11), Duration.ofSeconds(5),
                sequenceOf(FORCE_ACTIVE, 0.5, 0.5));

        assertThat(second.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);
        assertThat(second.nextState().activeFaultCode()).isNull();
    }
}
