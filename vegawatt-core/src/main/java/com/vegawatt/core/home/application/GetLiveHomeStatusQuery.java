package com.vegawatt.core.home.application;

import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetLiveHomeStatusQuery {

    private final HomeLiveStatePort homeLiveStatePort;
    private final ApplianceLiveStatePort applianceLiveStatePort;
    private final HomeRepository homeRepository;

    public GetLiveHomeStatusQuery(HomeLiveStatePort homeLiveStatePort, ApplianceLiveStatePort applianceLiveStatePort,
                                   HomeRepository homeRepository) {
        this.homeLiveStatePort = homeLiveStatePort;
        this.applianceLiveStatePort = applianceLiveStatePort;
        this.homeRepository = homeRepository;
    }

    public Optional<HomeLiveStatus> execute(UUID homeId) {
        return homeLiveStatePort.get(homeId)
                .flatMap(liveState -> homeRepository.findById(homeId)
                        .map(home -> new HomeLiveStatus(home, liveState, applianceLiveStatePort.getByHomeId(homeId))));
    }
}
