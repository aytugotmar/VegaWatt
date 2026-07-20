package com.vegawatt.core.history.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ConsumptionSnapshotJpaRepository extends JpaRepository<ConsumptionSnapshotEntity, UUID> {

    List<ConsumptionSnapshotEntity> findByHomeIdAndSnapshotTimeBetweenOrderBySnapshotTimeAsc(UUID homeId,
                                                                                               Instant from,
                                                                                               Instant to);
}
