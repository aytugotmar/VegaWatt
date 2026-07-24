package com.vegawatt.core.home.application;

import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogItem;
import com.vegawatt.core.appliancecatalog.domain.ApplianceCatalogRepository;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.ApplianceRepository;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import java.util.HashMap;
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
    private final ApplianceRepository applianceRepository;
    private final ApplianceCatalogRepository applianceCatalogRepository;

    public GetLiveHomeStatusQuery(HomeLiveStatePort homeLiveStatePort, ApplianceLiveStatePort applianceLiveStatePort,
                                   ApplianceRepository applianceRepository,
                                   ApplianceCatalogRepository applianceCatalogRepository) {
        this.homeLiveStatePort = homeLiveStatePort;
        this.applianceLiveStatePort = applianceLiveStatePort;
        this.applianceRepository = applianceRepository;
        this.applianceCatalogRepository = applianceCatalogRepository;
    }

    public Optional<HomeLiveStatus> execute(UUID homeId) {
        return homeLiveStatePort.get(homeId)
                .map(liveState -> {
                    List<ApplianceLiveState> appliances = applianceLiveStatePort.getByHomeId(homeId);
                    return new HomeLiveStatus(liveState, appliances, buildCatalogInfo(homeId));
                });
    }

    // Cosmetics (display name/icon) are resolved live against the current catalog rather than
    // from the appliance's own snapshot, unlike the simulation-critical snapshot fields — a
    // renamed/re-iconed catalog entry should be reflected immediately in the UI.
    private Map<UUID, ApplianceCatalogView> buildCatalogInfo(UUID homeId) {
        Map<UUID, ApplianceCatalogItem> catalogById = applianceCatalogRepository.findAllEnabled().stream()
                .collect(Collectors.toMap(ApplianceCatalogItem::id, item -> item));

        Map<UUID, ApplianceCatalogView> result = new HashMap<>();
        for (Appliance appliance : applianceRepository.findAllByHomeId(homeId)) {
            String catalogCode = appliance.catalogCodeSnapshot() == null ? null
                    : appliance.catalogCodeSnapshot().value();
            ApplianceCatalogItem catalogItem = appliance.catalogItemId() == null ? null
                    : catalogById.get(appliance.catalogItemId());
            result.put(appliance.id(), new ApplianceCatalogView(catalogCode,
                    catalogItem == null ? null : catalogItem.displayName(),
                    catalogItem == null ? null : catalogItem.iconKey()));
        }
        return result;
    }
}
