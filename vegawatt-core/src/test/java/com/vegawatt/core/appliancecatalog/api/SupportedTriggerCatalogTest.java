package com.vegawatt.core.appliancecatalog.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.common.ApplianceTriggerType;
import org.junit.jupiter.api.Test;

class SupportedTriggerCatalogTest {

    @Test
    void standbyDeviceResolvesToImplementedTriggersOnlyFilteringOutUnimplementedOnes() {
        // Spec §19 also lists SESSION_DURATION_EXCEEDED for this profile, but it's not implemented
        // yet, so it must not appear in the response. TELEMETRY_STALE now is (Aşama 8B).
        assertThat(SupportedTriggerCatalog.resolve(ApplianceBehaviorProfile.STANDBY_DEVICE))
                .containsExactly(ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED,
                        ApplianceTriggerType.UNUSUAL_STANDBY_CONSUMPTION, ApplianceTriggerType.TELEMETRY_STALE);
    }

    @Test
    void alwaysOnStableResolvesToItsFullSpecListNowThatTelemetryHealthIsImplemented() {
        assertThat(SupportedTriggerCatalog.resolve(ApplianceBehaviorProfile.ALWAYS_ON_STABLE))
                .containsExactly(ApplianceTriggerType.SAFE_POWER_LIMIT_BREACHED,
                        ApplianceTriggerType.TELEMETRY_STALE, ApplianceTriggerType.TELEMETRY_OFFLINE,
                        ApplianceTriggerType.TELEMETRY_RESUMED);
    }

    @Test
    void unmappedProfileResolvesToAnEmptyList() {
        assertThat(SupportedTriggerCatalog.resolve(ApplianceBehaviorProfile.FLOW_TRIGGERED)).isEmpty();
    }
}
