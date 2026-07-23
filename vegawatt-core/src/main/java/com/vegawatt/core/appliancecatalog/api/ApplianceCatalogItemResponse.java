package com.vegawatt.core.appliancecatalog.api;

import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCategory;
import com.vegawatt.core.common.ApplianceTriggerType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

record ApplianceCatalogItemResponse(
        UUID id,
        String code,
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
        boolean featured,
        List<ApplianceTriggerType> supportedTriggers) {

    static ApplianceCatalogItemResponse from(ApplianceCatalogItem item) {
        return new ApplianceCatalogItemResponse(
                item.id(),
                item.code().value(),
                item.displayName(),
                item.description(),
                item.category(),
                item.behaviorProfile(),
                item.defaultSafePowerLimitWatt(),
                item.defaultActiveMinWatt(),
                item.defaultActiveMaxWatt(),
                item.defaultStandbyMinWatt(),
                item.defaultStandbyMaxWatt(),
                item.supportsStandby(),
                item.supportsSchedule(),
                item.supportsOperatingModes(),
                item.iconKey(),
                item.featured(),
                SupportedTriggerCatalog.resolve(item.behaviorProfile()));
    }
}
