package com.vegawatt.core.home.application;

import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;

public record HomeLiveSummary(Home home, HomeLiveState liveState) {
}
