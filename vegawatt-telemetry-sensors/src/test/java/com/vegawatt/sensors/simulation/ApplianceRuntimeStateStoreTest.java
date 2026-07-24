package com.vegawatt.sensors.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplianceRuntimeStateStoreTest {

    private static ApplianceRuntimeState state(ApplianceOperatingState operatingState) {
        Instant now = Instant.now();
        return new ApplianceRuntimeState(operatingState, "NORMAL", now, null, null, null, null, now, null, null, null,
                0, null);
    }

    @Test
    void returnsEmptyForUnknownAppliance() {
        ApplianceRuntimeStateStore store = new ApplianceRuntimeStateStore();

        assertThat(store.get(UUID.randomUUID())).isEmpty();
    }

    @Test
    void returnsThePreviouslyStoredState() {
        ApplianceRuntimeStateStore store = new ApplianceRuntimeStateStore();
        UUID applianceId = UUID.randomUUID();
        ApplianceRuntimeState state = state(ApplianceOperatingState.ACTIVE);

        store.put(applianceId, state);

        assertThat(store.get(applianceId)).contains(state);
    }

    @Test
    void overwritesThePreviousStateOnPut() {
        ApplianceRuntimeStateStore store = new ApplianceRuntimeStateStore();
        UUID applianceId = UUID.randomUUID();
        store.put(applianceId, state(ApplianceOperatingState.ACTIVE));

        ApplianceRuntimeState updated = state(ApplianceOperatingState.STANDBY);
        store.put(applianceId, updated);

        assertThat(store.get(applianceId)).contains(updated);
    }
}
