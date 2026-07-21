package com.vegawatt.core.home.application;

import com.vegawatt.core.home.domain.HomeLiveStatePort;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetAllLiveHomesQuery {

    private final HomeLiveStatePort homeLiveStatePort;

    public GetAllLiveHomesQuery(HomeLiveStatePort homeLiveStatePort) {
        this.homeLiveStatePort = homeLiveStatePort;
    }

    public List<HomeLiveSummary> execute() {
        return homeLiveStatePort.getAll().stream()
                .map(HomeLiveSummary::new)
                .toList();
    }

    public List<HomeLiveSummary> execute(Set<UUID> restrictToHomeIds) {
        return homeLiveStatePort.getAll(restrictToHomeIds).stream()
                .map(HomeLiveSummary::new)
                .toList();
    }
}
