package com.vegawatt.core.history.application;

import com.vegawatt.core.common.time.BusinessTimeZone;
import com.vegawatt.core.history.domain.ConsumptionSnapshot;
import com.vegawatt.core.history.domain.ConsumptionSnapshotRepository;
import com.vegawatt.core.history.domain.HistoryGranularity;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetHomeConsumptionHistoryQuery {

    private final ConsumptionSnapshotRepository consumptionSnapshotRepository;

    public GetHomeConsumptionHistoryQuery(ConsumptionSnapshotRepository consumptionSnapshotRepository) {
        this.consumptionSnapshotRepository = consumptionSnapshotRepository;
    }

    public List<ConsumptionSnapshot> execute(UUID homeId, Instant from, Instant to) {
        return execute(homeId, from, to, HistoryGranularity.AUTO);
    }

    public List<ConsumptionSnapshot> execute(UUID homeId, Instant from, Instant to, HistoryGranularity requestedGranularity) {
        List<ConsumptionSnapshot> raw = consumptionSnapshotRepository.findByHomeIdAndSnapshotTimeBetween(homeId, from, to);
        if (raw.isEmpty()) {
            return raw;
        }

        HistoryGranularity effective = resolveGranularity(from, to, requestedGranularity);
        if (effective == HistoryGranularity.RAW) {
            return raw;
        }

        return aggregateSnapshots(raw, effective);
    }

    private HistoryGranularity resolveGranularity(Instant from, Instant to, HistoryGranularity requested) {
        if (requested != null && requested != HistoryGranularity.AUTO) {
            return requested;
        }

        Duration duration = Duration.between(from, to);
        if (duration.toHours() <= 2) {
            return HistoryGranularity.RAW;
        } else if (duration.toHours() <= 24) {
            return HistoryGranularity.FIVE_MINUTES;
        } else if (duration.toDays() <= 7) {
            return HistoryGranularity.HOUR;
        } else {
            return HistoryGranularity.DAY;
        }
    }

    private List<ConsumptionSnapshot> aggregateSnapshots(List<ConsumptionSnapshot> raw, HistoryGranularity granularity) {
        Map<Instant, ConsumptionSnapshot> buckets = new LinkedHashMap<>();

        for (ConsumptionSnapshot snapshot : raw) {
            Instant bucketKey = bucketKey(snapshot.snapshotTime(), granularity);
            buckets.put(bucketKey, new ConsumptionSnapshot(
                    snapshot.id(),
                    snapshot.homeId(),
                    bucketKey,
                    snapshot.accumulatedEnergyKwh(),
                    snapshot.accumulatedCost(),
                    snapshot.tariffState(),
                    snapshot.billingPeriod()
            ));
        }

        return new ArrayList<>(buckets.values());
    }

    private Instant bucketKey(Instant timestamp, HistoryGranularity granularity) {
        // DAY buckets must follow the customer's wall clock (Europe/Istanbul), not UTC — a
        // reading at 01:00 Turkey time is already the next calendar day locally even though it's
        // still the previous day in UTC. HOUR/FIVE_MINUTES bucket boundaries are unaffected by
        // this since Istanbul's offset is a fixed +3 with no DST, but using the same zone
        // everywhere avoids ever having to reason about which granularities are exempt.
        ZonedDateTime zdt = timestamp.atZone(BusinessTimeZone.ZONE);
        return switch (granularity) {
            case FIVE_MINUTES -> {
                int min = (zdt.getMinute() / 5) * 5;
                yield zdt.withMinute(min).withSecond(0).withNano(0).toInstant();
            }
            case HOUR -> zdt.withMinute(0).withSecond(0).withNano(0).toInstant();
            case DAY -> zdt.withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant();
            default -> timestamp;
        };
    }
}
