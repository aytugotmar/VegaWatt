package com.vegawatt.core.home.application;

import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GetAllLiveHomesQuery {

    private static final Logger log = LoggerFactory.getLogger(GetAllLiveHomesQuery.class);

    private final HomeLiveStatePort homeLiveStatePort;
    private final HomeRepository homeRepository;

    public GetAllLiveHomesQuery(HomeLiveStatePort homeLiveStatePort, HomeRepository homeRepository) {
        this.homeLiveStatePort = homeLiveStatePort;
        this.homeRepository = homeRepository;
    }

    public List<HomeLiveSummary> execute() {
        return homeLiveStatePort.getAll().stream()
                .flatMap(liveState -> toSummary(liveState).stream())
                .toList();
    }

    private Optional<HomeLiveSummary> toSummary(HomeLiveState liveState) {
        Optional<HomeLiveSummary> summary = homeRepository.findById(liveState.homeId())
                .map(home -> new HomeLiveSummary(home, liveState));
        if (summary.isEmpty()) {
            log.warn("Live state present in Ignite for unknown home {}", liveState.homeId());
        }
        return summary;
    }
}
