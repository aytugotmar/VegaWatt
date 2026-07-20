package com.vegawatt.sensors.registration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class HomeRegistry {

    private final ConcurrentHashMap<UUID, ApplianceConfig> appliances = new ConcurrentHashMap<>();

    public void upsert(ApplianceConfig config) {
        appliances.put(config.applianceId(), config);
    }

    public Optional<ApplianceConfig> find(UUID applianceId) {
        return Optional.ofNullable(appliances.get(applianceId));
    }

    public List<UUID> registeredApplianceIds() {
        return List.copyOf(appliances.keySet());
    }
}
