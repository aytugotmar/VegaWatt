package com.vegawatt.core.home.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.ApplianceHealthStatus;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
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

    @Mock
    private HomeLiveStatePort homeLiveStatePort;
    @Mock
    private ApplianceLiveStatePort applianceLiveStatePort;

    private GetLiveHomeStatusQuery query() {
        return new GetLiveHomeStatusQuery(homeLiveStatePort, applianceLiveStatePort);
    }

    private static ApplianceLiveState liveState(UUID applianceId, String catalogCode, String catalogDisplayName,
                                                 String catalogIconKey) {
        return new ApplianceLiveState(HOME_ID, applianceId, "Buzdolabı", "REFRIGERATOR", new BigDecimal("250"),
                new BigDecimal("120"), null, null, BigDecimal.ZERO.setScale(9), 0, 0, false, 0, 0, false,
                ApplianceHealthStatus.NORMAL, NOW, null, 0L, 0L, catalogCode, catalogDisplayName, catalogIconKey);
    }

    @Test
    void resolvesCatalogCodeDisplayNameAndIconKeyFromTheLiveStateItself() {
        UUID applianceId = UUID.randomUUID();
        when(homeLiveStatePort.get(HOME_ID)).thenReturn(Optional.of(HomeLiveState.zero(HOME_ID, "Ev", NOW)));
        when(applianceLiveStatePort.getByHomeId(HOME_ID))
                .thenReturn(List.of(liveState(applianceId, "REFRIGERATOR", "Buzdolabı", "refrigerator")));

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
        when(applianceLiveStatePort.getByHomeId(HOME_ID)).thenReturn(List.of(liveState(applianceId, null, null, null)));

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
