package com.vegawatt.core.appliancecatalog.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplianceCatalogItem(
        UUID id,
        ApplianceCatalogCode code,
        String displayName,
        String description,
        ApplianceCategory category,
        ApplianceBehaviorProfile behaviorProfile,
        BigDecimal defaultSafePowerLimitWatt,
        BigDecimal defaultActiveMinWatt,
        BigDecimal defaultActiveMaxWatt,
        BigDecimal defaultStandbyMinWatt,
        BigDecimal defaultStandbyMaxWatt,
        boolean supportsStandby,
        boolean supportsSchedule,
        boolean supportsOperatingModes,
        String iconKey,
        String searchKeywords,
        boolean featured,
        boolean enabled,
        int displayOrder) {
}
