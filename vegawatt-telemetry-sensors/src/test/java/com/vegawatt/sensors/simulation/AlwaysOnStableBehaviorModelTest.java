package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AlwaysOnStableBehaviorModelTest {

    private static final RandomSource MID_RANGE = () -> 0.5;
    private static final ZonedDateTime NOON = ZonedDateTime.now().withHour(12).withMinute(0).withSecond(0);

    private static ApplianceConfig routerConfig() {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "ROUTER", new BigDecimal("40"),
                new BigDecimal("5"), new BigDecimal("30"), "ROUTER", "ALWAYS_ON_STABLE", null, null);
    }

    private static RandomSource sequenceOf(double... values) {
        AtomicInteger index = new AtomicInteger(0);
        return () -> values[index.getAndIncrement()];
    }

    @Test
    void powerStaysWithinTheSimulationRangeAndStateIsAlwaysActive() {
        AlwaysOnStableBehaviorModel model = new AlwaysOnStableBehaviorModel(new SimulationProperties(5, 0.0));
        ApplianceConfig config = routerConfig();

        var reading = model.generate(config, null, NOON, Duration.ofSeconds(5), MID_RANGE);

        assertThat(reading.powerWatt()).isBetween(config.simulationMinWatt(), config.simulationMaxWatt());
        assertThat(reading.nextState().operatingState()).isEqualTo(ApplianceOperatingState.ACTIVE);
        assertThat(reading.nextState().activeFaultCode()).isNull();
    }

    @Test
    void preservesStateStartedAtAcrossTicks() {
        AlwaysOnStableBehaviorModel model = new AlwaysOnStableBehaviorModel(new SimulationProperties(5, 0.0));
        ApplianceConfig config = routerConfig();

        var first = model.generate(config, null, NOON, Duration.ofSeconds(5), MID_RANGE);
        var second = model.generate(config, first.nextState(), NOON.plusSeconds(5), Duration.ofSeconds(5),
                MID_RANGE);

        assertThat(second.nextState().stateStartedAt()).isEqualTo(first.nextState().stateStartedAt());
    }

    @Test
    void forcedFaultProducesPowerAboveTheSafeLimit() {
        AlwaysOnStableBehaviorModel model = new AlwaysOnStableBehaviorModel(new SimulationProperties(5, 1.0));
        ApplianceConfig config = routerConfig();

        var reading = model.generate(config, null, NOON, Duration.ofSeconds(5), sequenceOf(0.0, 0.5, 0.5));

        assertThat(reading.powerWatt()).isGreaterThan(config.safePowerLimitWatt());
        assertThat(reading.nextState().activeFaultCode()).isEqualTo("POWER_SPIKE_MALFUNCTION");
        assertThat(reading.nextState().faultExpectedEndAt()).isAfter(NOON.toInstant());
    }

    @Test
    void faultContinuesUnchangedWhileStillWithinItsWindow() {
        AlwaysOnStableBehaviorModel model = new AlwaysOnStableBehaviorModel(new SimulationProperties(5, 1.0));
        ApplianceConfig config = routerConfig();

        var first = model.generate(config, null, NOON, Duration.ofSeconds(5), sequenceOf(0.0, 0.5, 0.5));
        var second = model.generate(config, first.nextState(), NOON.plusSeconds(5), Duration.ofSeconds(5),
                sequenceOf(0.5));

        assertThat(second.powerWatt()).isGreaterThan(config.safePowerLimitWatt());
        assertThat(second.nextState().activeFaultCode()).isEqualTo("POWER_SPIKE_MALFUNCTION");
        assertThat(second.nextState().faultExpectedEndAt()).isEqualTo(first.nextState().faultExpectedEndAt());
    }

    @Test
    void faultClearsAndPowerReturnsToNormalAfterItExpires() {
        AlwaysOnStableBehaviorModel faultingModel = new AlwaysOnStableBehaviorModel(new SimulationProperties(5, 1.0));
        AlwaysOnStableBehaviorModel normalModel = new AlwaysOnStableBehaviorModel(new SimulationProperties(5, 0.0));
        ApplianceConfig config = routerConfig();

        var first = faultingModel.generate(config, null, NOON, Duration.ofSeconds(5), sequenceOf(0.0, 0.5, 0.5));
        var second = normalModel.generate(config, first.nextState(), NOON.plusSeconds(61), Duration.ofSeconds(5),
                MID_RANGE);

        assertThat(second.nextState().activeFaultCode()).isNull();
        assertThat(second.powerWatt()).isBetween(config.simulationMinWatt(), config.simulationMaxWatt());
    }
}
