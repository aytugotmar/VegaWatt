package com.vegawatt.core.home.application;

import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GetLiveHomeStatusQuery {

    private final HomeLiveStatePort homeLiveStatePort;
    private final ApplianceLiveStatePort applianceLiveStatePort;

    public GetLiveHomeStatusQuery(HomeLiveStatePort homeLiveStatePort, ApplianceLiveStatePort applianceLiveStatePort) {
        this.homeLiveStatePort = homeLiveStatePort;
        this.applianceLiveStatePort = applianceLiveStatePort;
    }

    // Pulls exclusively from Apache Ignite — no PostgreSQL read on this path. Catalog cosmetics
    // (display name/icon) are resolved once, at registration/reconciliation time, and stamped onto
    // the appliance's own ApplianceLiveState (see ApplianceFactory.resolveCatalogView) instead of
    // being re-queried from the catalog table on every ~2s poll.
    public Optional<HomeLiveStatus> execute(UUID homeId) {
        return homeLiveStatePort.get(homeId)
                .map(liveState -> {
                    List<ApplianceLiveState> appliances = applianceLiveStatePort.getByHomeId(homeId);
                    Map<UUID, ApplianceCatalogView> catalogInfo = appliances.stream()
                            .collect(Collectors.toMap(ApplianceLiveState::applianceId,
                                    a -> new ApplianceCatalogView(a.catalogCode(), a.catalogDisplayName(),
                                            a.catalogIconKey())));
                    return new HomeLiveStatus(liveState, appliances, catalogInfo);
                });
    }
}
