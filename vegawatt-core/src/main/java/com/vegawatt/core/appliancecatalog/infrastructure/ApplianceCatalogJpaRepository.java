package com.vegawatt.core.appliancecatalog.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ApplianceCatalogJpaRepository extends JpaRepository<ApplianceCatalogEntity, UUID> {

    List<ApplianceCatalogEntity> findByEnabledTrue();

    Optional<ApplianceCatalogEntity> findByCodeAndEnabledTrue(String code);

    Optional<ApplianceCatalogEntity> findByIdAndEnabledTrue(UUID id);
}
