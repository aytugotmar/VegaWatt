package com.vegawatt.core.history.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import com.vegawatt.core.history.domain.ConsumptionSnapshot;
import com.vegawatt.core.history.domain.ConsumptionSnapshotRepository;
import com.vegawatt.core.history.domain.HistoryGranularity;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetHomeConsumptionHistoryQueryTest {

    private ConsumptionSnapshotRepository repository;
    private GetHomeConsumptionHistoryQuery query;
    private final UUID homeId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(ConsumptionSnapshotRepository.class);
        query = new GetHomeConsumptionHistoryQuery(repository);
    }

    @Test
    void execute_returnsRawSnapshotsForShortTimeRange() {
        Instant start = Instant.parse("2026-07-22T10:00:00Z");
        Instant end = start.plus(1, ChronoUnit.HOURS);

        List<ConsumptionSnapshot> rawSnapshots = List.of(
                new ConsumptionSnapshot(UUID.randomUUID(), homeId, start, new BigDecimal("1.0"), Money.zero(), TariffState.BASE, "2026-07"),
                new ConsumptionSnapshot(UUID.randomUUID(), homeId, start.plus(30, ChronoUnit.MINUTES), new BigDecimal("2.0"), Money.zero(), TariffState.BASE, "2026-07")
        );

        when(repository.findByHomeIdAndSnapshotTimeBetween(eq(homeId), any(), any())).thenReturn(rawSnapshots);

        List<ConsumptionSnapshot> result = query.execute(homeId, start, end, HistoryGranularity.AUTO);

        assertThat(result).hasSize(2);
    }

    @Test
    void execute_bucketsHourlyForMultiDayRange() {
        Instant start = Instant.parse("2026-07-20T00:00:00Z");
        Instant end = start.plus(3, ChronoUnit.DAYS);

        List<ConsumptionSnapshot> rawSnapshots = new ArrayList<>();
        // Generate 60 minute snapshots for 1 hour
        for (int i = 0; i < 60; i++) {
            rawSnapshots.add(new ConsumptionSnapshot(UUID.randomUUID(), homeId, start.plus(i, ChronoUnit.MINUTES),
                    new BigDecimal(i), Money.zero(), TariffState.BASE, "2026-07"));
        }

        when(repository.findByHomeIdAndSnapshotTimeBetween(eq(homeId), any(), any())).thenReturn(rawSnapshots);

        List<ConsumptionSnapshot> result = query.execute(homeId, start, end, HistoryGranularity.HOUR);

        // All 60 snapshots in that single hour bucket down into 1 single aggregated point
        assertThat(result).hasSize(1);
        assertThat(result.get(0).accumulatedEnergyKwh()).isEqualByComparingTo("59");
    }

    @Test
    void dayBucketingFollowsTheIstanbulCalendarDayNotUtc() {
        // 2026-07-25T22:30:00Z is 2026-07-26 01:30 in Europe/Istanbul (UTC+3) — already the next
        // calendar day locally even though it's still 25 July in UTC. A UTC-keyed bucket would
        // wrongly attribute this reading to 25 July.
        Instant lateNightIstanbul = Instant.parse("2026-07-25T22:30:00Z");
        Instant sameIstanbulDayLater = Instant.parse("2026-07-25T23:00:00Z"); // 2026-07-26 02:00 Istanbul

        List<ConsumptionSnapshot> rawSnapshots = List.of(
                new ConsumptionSnapshot(UUID.randomUUID(), homeId, lateNightIstanbul, new BigDecimal("10"),
                        Money.zero(), TariffState.BASE, "2026-07"),
                new ConsumptionSnapshot(UUID.randomUUID(), homeId, sameIstanbulDayLater, new BigDecimal("11"),
                        Money.zero(), TariffState.BASE, "2026-07"));
        when(repository.findByHomeIdAndSnapshotTimeBetween(eq(homeId), any(), any())).thenReturn(rawSnapshots);

        List<ConsumptionSnapshot> result = query.execute(homeId, lateNightIstanbul, sameIstanbulDayLater,
                HistoryGranularity.DAY);

        // Both readings fall in the same Istanbul calendar day (26 July), so they must collapse
        // into a single bucket — a UTC-keyed implementation would instead split them into two
        // separate days (25 July and 26 July).
        assertThat(result).hasSize(1);
        assertThat(result.get(0).accumulatedEnergyKwh()).isEqualByComparingTo("11");
        assertThat(result.get(0).snapshotTime())
                .isEqualTo(Instant.parse("2026-07-25T21:00:00Z")); // 2026-07-26T00:00:00 Istanbul
    }

    @Test
    void aggregationCarriesTheBillingPeriodOfTheLastSnapshotInEachBucket() {
        Instant start = Instant.parse("2026-08-01T00:30:00Z");

        List<ConsumptionSnapshot> rawSnapshots = List.of(
                new ConsumptionSnapshot(UUID.randomUUID(), homeId, start, new BigDecimal("5"), Money.zero(),
                        TariffState.BASE, "2026-08"),
                new ConsumptionSnapshot(UUID.randomUUID(), homeId, start.plus(10, ChronoUnit.MINUTES),
                        new BigDecimal("6"), Money.zero(), TariffState.BASE, "2026-08"));
        when(repository.findByHomeIdAndSnapshotTimeBetween(eq(homeId), any(), any())).thenReturn(rawSnapshots);

        List<ConsumptionSnapshot> result = query.execute(homeId, start, start.plus(1, ChronoUnit.HOURS),
                HistoryGranularity.HOUR);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).billingPeriod()).isEqualTo("2026-08");
    }
}
