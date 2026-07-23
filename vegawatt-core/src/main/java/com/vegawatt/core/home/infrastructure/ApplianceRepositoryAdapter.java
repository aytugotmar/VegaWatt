package com.vegawatt.core.home.infrastructure;

import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogCode;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ApplianceRepositoryAdapter implements ApplianceRepository {

    private final ApplianceJpaRepository jpaRepository;

    ApplianceRepositoryAdapter(ApplianceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Appliance> findById(UUID applianceId) {
        return jpaRepository.findById(applianceId).map(entity -> new Appliance(entity.getId(), entity.getHomeId(),
                entity.getName(), entity.getType(), entity.getSafePowerLimitWatt(), entity.getSimulationMinWatt(),
                entity.getSimulationMaxWatt(), entity.isActive(), entity.getCatalogItemId(),
                entity.getCatalogCodeSnapshot() == null ? null : new ApplianceCatalogCode(entity.getCatalogCodeSnapshot()),
                entity.getBehaviorProfileSnapshot() == null ? null
                        : ApplianceBehaviorProfile.valueOf(entity.getBehaviorProfileSnapshot()),
                entity.getStandbyMinWatt(), entity.getStandbyMaxWatt()));
    }
}
