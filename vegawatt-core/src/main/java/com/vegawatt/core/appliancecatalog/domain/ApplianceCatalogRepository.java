package com.vegawatt.core.appliancecatalog.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplianceCatalogRepository {

    List<ApplianceCatalogItem> findAllEnabled();

    Optional<ApplianceCatalogItem> findEnabledByCode(String code);

    Optional<ApplianceCatalogItem> findEnabledById(UUID id);
}
