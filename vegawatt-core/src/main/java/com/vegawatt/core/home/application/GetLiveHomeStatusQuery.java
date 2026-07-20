package com.vegawatt.core.home.application;

import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetLiveHomeStatusQuery {

    private final HomeLiveStatePort homeLiveStatePort;
    private final ApplianceLiveStatePort applianceLiveStatePort;

    public GetLiveHomeStatusQuery(HomeLiveStatePort homeLiveStatePort, ApplianceLiveStatePort applianceLiveStatePort) {
        this.homeLiveStatePort = homeLiveStatePort;
        this.applianceLiveStatePort = applianceLiveStatePort;
    }

    public Optional<HomeLiveStatus> execute(UUID homeId) {
        return homeLiveStatePort.get(homeId)
                .map(home -> new HomeLiveStatus(home, applianceLiveStatePort.getByHomeId(homeId)));
    }
}
