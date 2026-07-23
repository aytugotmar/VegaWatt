package com.vegawatt.core.appliancecatalog.application;

import com.vegawatt.core.appliancecatalog.domain.ApplianceCategory;

public record ApplianceCatalogFilter(ApplianceCategory category, String search, Boolean featured) {
}
