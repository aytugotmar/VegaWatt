package com.vegawatt.core.home.infrastructure;

import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class HomeRepositoryAdapter implements HomeRepository {

    private final HomeJpaRepository homeJpaRepository;
    private final ApplianceJpaRepository applianceJpaRepository;

    HomeRepositoryAdapter(HomeJpaRepository homeJpaRepository, ApplianceJpaRepository applianceJpaRepository) {
        this.homeJpaRepository = homeJpaRepository;
        this.applianceJpaRepository = applianceJpaRepository;
    }

    @Override
    public Home save(Home home) {
        homeJpaRepository.save(new HomeEntity(home.id(), home.name(), home.contactEmail(), home.energyQuotaKwh(),
                home.budgetQuotaTry(), home.baseTariffPerKwh(), home.penaltyTariffPerKwh(), home.createdAt(),
                home.updatedAt()));

        List<ApplianceEntity> applianceEntities = home.appliances().stream()
                .map(appliance -> new ApplianceEntity(appliance.id(), appliance.homeId(), appliance.name(),
                        appliance.type(), appliance.safePowerLimitWatt(), appliance.simulationMinWatt(),
                        appliance.simulationMaxWatt(), appliance.active(), home.createdAt()))
                .toList();
        applianceJpaRepository.saveAll(applianceEntities);

        return home;
    }

    @Override
    public Optional<Home> findById(UUID id) {
        return homeJpaRepository.findById(id).map(homeEntity -> {
            List<Appliance> appliances = applianceJpaRepository.findByHomeId(id).stream()
                    .map(HomeRepositoryAdapter::toDomainAppliance)
                    .toList();
            return Home.reconstitute(homeEntity.getId(), homeEntity.getName(), homeEntity.getContactEmail(),
                    homeEntity.getEnergyQuotaKwh(), homeEntity.getBudgetQuotaTry(), homeEntity.getBaseTariffPerKwh(),
                    homeEntity.getPenaltyTariffPerKwh(), homeEntity.getCreatedAt(), homeEntity.getUpdatedAt(),
                    appliances);
        });
    }

    private static Appliance toDomainAppliance(ApplianceEntity entity) {
        return new Appliance(entity.getId(), entity.getHomeId(), entity.getName(), entity.getType(),
                entity.getSafePowerLimitWatt(), entity.getSimulationMinWatt(), entity.getSimulationMaxWatt(),
                entity.isActive());
    }
}
