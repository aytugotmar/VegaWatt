package com.vegawatt.core.history.infrastructure;

import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import com.vegawatt.core.history.domain.ConsumptionSnapshot;
import com.vegawatt.core.history.domain.ConsumptionSnapshotRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ConsumptionSnapshotRepositoryAdapter implements ConsumptionSnapshotRepository {

    private final ConsumptionSnapshotJpaRepository jpaRepository;

    ConsumptionSnapshotRepositoryAdapter(ConsumptionSnapshotJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ConsumptionSnapshot save(ConsumptionSnapshot snapshot) {
        ConsumptionSnapshotEntity saved = jpaRepository.save(toEntity(snapshot));
        return toDomain(saved);
    }

    @Override
    public List<ConsumptionSnapshot> findByHomeIdAndSnapshotTimeBetween(UUID homeId, Instant from, Instant to) {
        return jpaRepository.findByHomeIdAndSnapshotTimeBetweenOrderBySnapshotTimeAsc(homeId, from, to).stream()
                .map(ConsumptionSnapshotRepositoryAdapter::toDomain)
                .toList();
    }

    private static ConsumptionSnapshotEntity toEntity(ConsumptionSnapshot snapshot) {
        return new ConsumptionSnapshotEntity(snapshot.id(), snapshot.homeId(), snapshot.snapshotTime(),
                snapshot.accumulatedEnergyKwh(), snapshot.accumulatedCost().amount(), snapshot.tariffState().name());
    }

    private static ConsumptionSnapshot toDomain(ConsumptionSnapshotEntity entity) {
        return new ConsumptionSnapshot(entity.getId(), entity.getHomeId(), entity.getSnapshotTime(),
                entity.getAccumulatedEnergyKwh(), Money.of(entity.getAccumulatedCost()),
                TariffState.valueOf(entity.getTariffState()));
    }
}
