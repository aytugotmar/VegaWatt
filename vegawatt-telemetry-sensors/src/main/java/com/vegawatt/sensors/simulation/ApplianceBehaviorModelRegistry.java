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
    private final LegacyFallbackBehaviorModel fallbackModel;

    ApplianceBehaviorModelRegistry(List<ApplianceBehaviorModel> models, LegacyFallbackBehaviorModel fallbackModel) {
        this.modelsByProfile = models.stream()
                .filter(m -> m.supportedProfile() != null)
                .collect(Collectors.toUnmodifiableMap(ApplianceBehaviorModel::supportedProfile, model -> model));
        this.fallbackModel = fallbackModel;
    }

    Optional<ApplianceBehaviorModel> forConfig(ApplianceConfig config) {
        if (config.behaviorProfile() == null) {
            return Optional.of(fallbackModel);
        }
        try {
            ApplianceBehaviorProfile profile = ApplianceBehaviorProfile.valueOf(config.behaviorProfile());
            ApplianceBehaviorModel model = modelsByProfile.get(profile);
            return Optional.ofNullable(model != null ? model : fallbackModel);
        } catch (IllegalArgumentException e) {
            return Optional.of(fallbackModel);
        }
    }
}
