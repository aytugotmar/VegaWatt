package com.vegawatt.core.history.application;

import com.vegawatt.core.history.domain.ConsumptionSnapshot;
import com.vegawatt.core.history.domain.ConsumptionSnapshotRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetHomeConsumptionHistoryQuery {

    private final ConsumptionSnapshotRepository consumptionSnapshotRepository;

    public GetHomeConsumptionHistoryQuery(ConsumptionSnapshotRepository consumptionSnapshotRepository) {
        this.consumptionSnapshotRepository = consumptionSnapshotRepository;
    }

    public List<ConsumptionSnapshot> execute(UUID homeId, Instant from, Instant to) {
        return consumptionSnapshotRepository.findByHomeIdAndSnapshotTimeBetween(homeId, from, to);
    }
}
