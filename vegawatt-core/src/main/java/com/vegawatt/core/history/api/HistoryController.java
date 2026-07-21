package com.vegawatt.core.history.api;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.history.application.GetHomeConsumptionHistoryQuery;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/homes/{homeId}/history")
class HistoryController {

    private static final int DEFAULT_RANGE_HOURS = 24;

    private final GetHomeConsumptionHistoryQuery getHomeConsumptionHistoryQuery;
    private final ClockProvider clockProvider;

    HistoryController(GetHomeConsumptionHistoryQuery getHomeConsumptionHistoryQuery, ClockProvider clockProvider) {
        this.getHomeConsumptionHistoryQuery = getHomeConsumptionHistoryQuery;
        this.clockProvider = clockProvider;
    }

    @GetMapping
    List<ConsumptionHistoryPointResponse> history(
            @PathVariable UUID homeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant rangeEnd = to != null ? to : clockProvider.now();
        Instant rangeStart = from != null ? from : rangeEnd.minus(DEFAULT_RANGE_HOURS, ChronoUnit.HOURS);

        return getHomeConsumptionHistoryQuery.execute(homeId, rangeStart, rangeEnd).stream()
                .map(ConsumptionHistoryPointResponse::from)
                .toList();
    }
}
