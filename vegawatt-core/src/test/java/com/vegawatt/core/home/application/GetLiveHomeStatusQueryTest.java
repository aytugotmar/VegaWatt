package com.vegawatt.core.home.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogCode;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogRepository;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCategory;
import com.vegawatt.core.common.ApplianceHealthStatus;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.ApplianceRepository;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetLiveHomeStatusQueryTest {

    private static final Instant NOW = Instant.parse("2026-07-24T10:00:00Z");
    private static final UUID HOME_ID = UUID.randomUUID();
    private static final UUID CATALOG_ITEM_ID = UUID.randomUUID();

    @Mock
    private HomeLiveStatePort homeLiveStatePort;
    @Mock
    private ApplianceLiveStatePort applianceLiveStatePort;
    @Mock
    private ApplianceRepository applianceRepository;
    @Mock
    private ApplianceCatalogRepository applianceCatalogRepository;

    private GetLiveHomeStatusQuery query() {
        return new GetLiveHomeStatusQuery(homeLiveStatePort, applianceLiveStatePort, applianceRepository,
                applianceCatalogRepository);
    }

    private static ApplianceCatalogItem catalogItem() {
        return new ApplianceCatalogItem(CATALOG_ITEM_ID, new ApplianceCatalogCode("REFRIGERATOR"), "Buzdolabı",
                "description", ApplianceCategory.KITCHEN, ApplianceBehaviorProfile.THERMOSTATIC_CYCLE,
                new BigDecimal("250"), new BigDecimal("30"), new BigDecimal("160"), BigDecimal.ZERO,
                new BigDecimal("2"), true, false, false, "refrigerator", null, true, true, 1);
    }

    private static ApplianceLiveState liveState(UUID applianceId) {
        return new ApplianceLiveState(HOME_ID, applianceId, "Buzdolabı", "REFRIGERATOR", new BigDecimal("250"),
                new BigDecimal("120"), null, null, BigDecimal.ZERO.setScale(9), 0, 0, false, 0, 0, false,
                ApplianceHealthStatus.NORMAL, NOW, null);
    }

    @Test
    void resolvesCatalogCodeDisplayNameAndIconKeyForACatalogAppliance() {
        UUID applianceId = UUID.randomUUID();
        when(homeLiveStatePort.get(HOME_ID)).thenReturn(Optional.of(HomeLiveState.zero(HOME_ID, "Ev", NOW)));
        when(applianceLiveStatePort.getByHomeId(HOME_ID)).thenReturn(List.of(liveState(applianceId)));
        when(applianceCatalogRepository.findAllEnabled()).thenReturn(List.of(catalogItem()));
        Appliance appliance = new Appliance(applianceId, HOME_ID, "Buzdolabı", "REFRIGERATOR",
                new BigDecimal("250"), new BigDecimal("30"), new BigDecimal("160"), true, CATALOG_ITEM_ID,
                new ApplianceCatalogCode("REFRIGERATOR"), ApplianceBehaviorProfile.THERMOSTATIC_CYCLE,
                BigDecimal.ZERO, new BigDecimal("2"));
        when(applianceRepository.findAllByHomeId(HOME_ID)).thenReturn(List.of(appliance));

        HomeLiveStatus result = query().execute(HOME_ID).orElseThrow();

        ApplianceCatalogView view = result.catalogInfoByApplianceId().get(applianceId);
        assertThat(view.catalogCode()).isEqualTo("REFRIGERATOR");
        assertThat(view.catalogDisplayName()).isEqualTo("Buzdolabı");
        assertThat(view.catalogIconKey()).isEqualTo("refrigerator");
    }

    @Test
    void appliancesWithoutACatalogItemGetNullCatalogFields() {
        UUID applianceId = UUID.randomUUID();
        when(homeLiveStatePort.get(HOME_ID)).thenReturn(Optional.of(HomeLiveState.zero(HOME_ID, "Ev", NOW)));
        when(applianceLiveStatePort.getByHomeId(HOME_ID)).thenReturn(List.of(liveState(applianceId)));
        when(applianceCatalogRepository.findAllEnabled()).thenReturn(List.of());
        Appliance customAppliance = new Appliance(applianceId, HOME_ID, "Özel Cihaz", "CUSTOM",
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("80"), true, null, null, null, null,
                null);
        when(applianceRepository.findAllByHomeId(HOME_ID)).thenReturn(List.of(customAppliance));

        HomeLiveStatus result = query().execute(HOME_ID).orElseThrow();

        ApplianceCatalogView view = result.catalogInfoByApplianceId().get(applianceId);
        assertThat(view.catalogCode()).isNull();
        assertThat(view.catalogDisplayName()).isNull();
        assertThat(view.catalogIconKey()).isNull();
    }

    @Test
    void returnsEmptyWhenHomeLiveStateIsMissing() {
        when(homeLiveStatePort.get(HOME_ID)).thenReturn(Optional.empty());

        assertThat(query().execute(HOME_ID)).isEmpty();
    }
}
