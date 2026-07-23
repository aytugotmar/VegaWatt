package com.vegawatt.core.appliancecatalog.infrastructure;

import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogCode;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogRepository;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ApplianceCatalogRepositoryAdapter implements ApplianceCatalogRepository {

    private final ApplianceCatalogJpaRepository jpaRepository;

    ApplianceCatalogRepositoryAdapter(ApplianceCatalogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<ApplianceCatalogItem> findAllEnabled() {
        return jpaRepository.findByEnabledTrue().stream()
                .map(ApplianceCatalogRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<ApplianceCatalogItem> findEnabledByCode(String code) {
        return jpaRepository.findByCodeAndEnabledTrue(code).map(ApplianceCatalogRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<ApplianceCatalogItem> findEnabledById(UUID id) {
        return jpaRepository.findByIdAndEnabledTrue(id).map(ApplianceCatalogRepositoryAdapter::toDomain);
    }

    private static ApplianceCatalogItem toDomain(ApplianceCatalogEntity entity) {
        return new ApplianceCatalogItem(
                entity.getId(),
                new ApplianceCatalogCode(entity.getCode()),
                entity.getDisplayName(),
                entity.getDescription(),
                ApplianceCategory.valueOf(entity.getCategory()),
                ApplianceBehaviorProfile.valueOf(entity.getBehaviorProfile()),
                entity.getDefaultSafePowerLimitWatt(),
                entity.getDefaultActiveMinWatt(),
                entity.getDefaultActiveMaxWatt(),
                entity.getDefaultStandbyMinWatt(),
                entity.getDefaultStandbyMaxWatt(),
                entity.isSupportsStandby(),
                entity.isSupportsSchedule(),
                entity.isSupportsOperatingModes(),
                entity.getIconKey(),
                entity.getSearchKeywords(),
                entity.isFeatured(),
                entity.isEnabled(),
                entity.getDisplayOrder());
    }
}
