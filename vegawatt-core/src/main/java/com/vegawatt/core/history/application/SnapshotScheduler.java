package com.vegawatt.core.history.application;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.history.domain.ConsumptionSnapshot;
import com.vegawatt.core.history.domain.ConsumptionSnapshotRepository;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(SnapshotScheduler.class);

    private final HomeLiveStatePort homeLiveStatePort;
    private final ConsumptionSnapshotRepository consumptionSnapshotRepository;
    private final ClockProvider clockProvider;

    public SnapshotScheduler(HomeLiveStatePort homeLiveStatePort,
                              ConsumptionSnapshotRepository consumptionSnapshotRepository,
                              ClockProvider clockProvider) {
        this.homeLiveStatePort = homeLiveStatePort;
        this.consumptionSnapshotRepository = consumptionSnapshotRepository;
        this.clockProvider = clockProvider;
    }

    @Scheduled(fixedRateString = "${vegawatt.scheduling.snapshot-interval-seconds}", timeUnit = TimeUnit.SECONDS,
            scheduler = "snapshotTaskScheduler")
    public void captureSnapshots() {
        Instant now = clockProvider.now();
        for (HomeLiveState liveState : homeLiveStatePort.getAll()) {
            try {
                consumptionSnapshotRepository.save(ConsumptionSnapshot.create(liveState.homeId(), now,
                        liveState.currentEnergyKwh(), liveState.currentCost(), liveState.tariffState(),
                        liveState.billingPeriod()));
            } catch (RuntimeException e) {
                log.error("Failed to persist consumption snapshot for home {}", liveState.homeId(), e);
            }
        }
    }
}
