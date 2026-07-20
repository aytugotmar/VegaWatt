package com.vegawatt.core.history.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConsumptionSnapshotRepository {

    ConsumptionSnapshot save(ConsumptionSnapshot snapshot);

    List<ConsumptionSnapshot> findByHomeIdAndSnapshotTimeBetween(UUID homeId, Instant from, Instant to);
}
