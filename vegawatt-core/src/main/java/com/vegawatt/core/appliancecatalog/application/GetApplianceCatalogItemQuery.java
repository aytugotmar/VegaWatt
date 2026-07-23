package com.vegawatt.core.appliancecatalog.application;

import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItemNotFoundException;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogRepository;
import org.springframework.stereotype.Service;

@Service
public class GetApplianceCatalogItemQuery {

    private final ApplianceCatalogRepository repository;

    GetApplianceCatalogItemQuery(ApplianceCatalogRepository repository) {
        this.repository = repository;
    }

    public ApplianceCatalogItem execute(String code) {
        return repository.findEnabledByCode(code)
                .orElseThrow(() -> new ApplianceCatalogItemNotFoundException(code));
    }
}
