package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplianceBehaviorModelRegistryTest {

    private static ApplianceBehaviorModel modelFor(ApplianceBehaviorProfile profile) {
        ApplianceBehaviorModel model = mock(ApplianceBehaviorModel.class);
        when(model.supportedProfile()).thenReturn(profile);
        return model;
    }

    private static ApplianceConfig configWithProfile(String behaviorProfile) {
        return new ApplianceConfig(UUID.randomUUID(), UUID.randomUUID(), "SOME_TYPE", new BigDecimal("100"),
                new BigDecimal("10"), new BigDecimal("90"), null, behaviorProfile, null, null);
    }

    @Test
    void returnsTheModelRegisteredForAKnownProfile() {
        ApplianceBehaviorModel standbyModel = modelFor(ApplianceBehaviorProfile.STANDBY_DEVICE);
        ApplianceBehaviorModelRegistry registry = new ApplianceBehaviorModelRegistry(List.of(standbyModel));

        assertThat(registry.forConfig(configWithProfile("STANDBY_DEVICE"))).contains(standbyModel);
    }

    @Test
    void returnsEmptyWhenBehaviorProfileIsNull() {
        ApplianceBehaviorModelRegistry registry = new ApplianceBehaviorModelRegistry(
                List.of(modelFor(ApplianceBehaviorProfile.STANDBY_DEVICE)));

        assertThat(registry.forConfig(configWithProfile(null))).isEmpty();
    }

    @Test
    void returnsEmptyForAnUnparseableProfileString() {
        ApplianceBehaviorModelRegistry registry = new ApplianceBehaviorModelRegistry(
                List.of(modelFor(ApplianceBehaviorProfile.STANDBY_DEVICE)));

        assertThat(registry.forConfig(configWithProfile("NOT_A_REAL_PROFILE"))).isEmpty();
    }

    @Test
    void returnsEmptyForARecognizedProfileWithoutARegisteredModel() {
        ApplianceBehaviorModelRegistry registry = new ApplianceBehaviorModelRegistry(
                List.of(modelFor(ApplianceBehaviorProfile.STANDBY_DEVICE)));

        assertThat(registry.forConfig(configWithProfile("ALWAYS_ON_STABLE"))).isEmpty();
    }

    @Test
    void failsFastWhenTwoModelsClaimTheSameProfile() {
        ApplianceBehaviorModel first = modelFor(ApplianceBehaviorProfile.STANDBY_DEVICE);
        ApplianceBehaviorModel second = modelFor(ApplianceBehaviorProfile.STANDBY_DEVICE);

        assertThatThrownBy(() -> new ApplianceBehaviorModelRegistry(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class);
    }
}
