package com.vegawatt.core.home.application;

import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.HomeLiveState;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record HomeLiveStatus(HomeLiveState liveState, List<ApplianceLiveState> appliances,
                              Map<UUID, ApplianceCatalogView> catalogInfoByApplianceId) {
}
