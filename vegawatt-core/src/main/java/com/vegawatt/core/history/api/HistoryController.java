package com.vegawatt.core.history.api;

import com.vegawatt.core.access.domain.HomeAuthorizationService;
import com.vegawatt.core.common.config.HistoryProperties;
import com.vegawatt.core.common.security.CurrentUser;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.history.application.GetHomeConsumptionHistoryQuery;
import com.vegawatt.core.history.domain.InvalidHistoryRangeException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vegawatt.core.history.domain.HistoryGranularity;

@RestController
@RequestMapping("/api/v1/homes/{homeId}/history")
class HistoryController {

    private static final int DEFAULT_RANGE_HOURS = 24;

    private final GetHomeConsumptionHistoryQuery getHomeConsumptionHistoryQuery;
    private final ClockProvider clockProvider;
    private final HomeAuthorizationService homeAuthorizationService;
    private final HistoryProperties historyProperties;

    HistoryController(GetHomeConsumptionHistoryQuery getHomeConsumptionHistoryQuery, ClockProvider clockProvider,
                       HomeAuthorizationService homeAuthorizationService, HistoryProperties historyProperties) {
        this.getHomeConsumptionHistoryQuery = getHomeConsumptionHistoryQuery;
        this.clockProvider = clockProvider;
        this.homeAuthorizationService = homeAuthorizationService;
        this.historyProperties = historyProperties;
    }

    @GetMapping
    List<ConsumptionHistoryPointResponse> history(
            @PathVariable UUID homeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String granularity,
            @AuthenticationPrincipal CurrentUser currentUser) {
        homeAuthorizationService.requireAccess(currentUser, homeId);
        Instant rangeEnd = to != null ? to : clockProvider.now();
        Instant rangeStart = from != null ? from : rangeEnd.minus(DEFAULT_RANGE_HOURS, ChronoUnit.HOURS);

        if (rangeStart.isAfter(rangeEnd)) {
            throw new InvalidHistoryRangeException("'from' must not be after 'to'");
        }
        long maxRangeDays = historyProperties.maxRangeDays();
        if (Duration.between(rangeStart, rangeEnd).toDays() > maxRangeDays) {
            throw new InvalidHistoryRangeException(
                    "Requested range exceeds the maximum of " + maxRangeDays + " days");
        }

        HistoryGranularity requestedGranularity = parseGranularity(granularity);

        return getHomeConsumptionHistoryQuery.execute(homeId, rangeStart, rangeEnd, requestedGranularity).stream()
                .map(ConsumptionHistoryPointResponse::from)
                .toList();
    }

    private static HistoryGranularity parseGranularity(String value) {
        if (value == null || value.isBlank()) {
            return HistoryGranularity.AUTO;
        }
        try {
            return HistoryGranularity.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return HistoryGranularity.AUTO;
        }
    }
}
