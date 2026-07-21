package com.vegawatt.core.home.application;

import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.HomeLiveState;
import java.util.List;

public record HomeLiveStatus(HomeLiveState liveState, List<ApplianceLiveState> appliances) {
}
