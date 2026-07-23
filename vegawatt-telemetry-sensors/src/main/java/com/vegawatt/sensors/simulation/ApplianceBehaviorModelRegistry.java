package com.vegawatt.sensors.simulation;

import com.vegawatt.sensors.registration.ApplianceConfig;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class ApplianceBehaviorModelRegistry {

    private final Map<ApplianceBehaviorProfile, ApplianceBehaviorModel> modelsByProfile;

    ApplianceBehaviorModelRegistry(List<ApplianceBehaviorModel> models) {
        this.modelsByProfile = models.stream()
                .collect(Collectors.toUnmodifiableMap(ApplianceBehaviorModel::supportedProfile, model -> model));
    }

    /** Empty when the appliance has no catalog association, an unrecognized profile string, or a
     * profile no model has been implemented for yet — callers fall back to the legacy stateless
     * generator in all of those cases. */
    Optional<ApplianceBehaviorModel> forConfig(ApplianceConfig config) {
        if (config.behaviorProfile() == null) {
            return Optional.empty();
        }
        try {
            ApplianceBehaviorProfile profile = ApplianceBehaviorProfile.valueOf(config.behaviorProfile());
            return Optional.ofNullable(modelsByProfile.get(profile));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
