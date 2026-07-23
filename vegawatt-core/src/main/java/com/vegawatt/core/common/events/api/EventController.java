package com.vegawatt.core.common.events.api;

import com.vegawatt.core.common.events.application.GetHomeEventsQuery;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/homes/{homeId}/events")
class EventController {

    private final GetHomeEventsQuery getHomeEventsQuery;

    EventController(GetHomeEventsQuery getHomeEventsQuery) {
        this.getHomeEventsQuery = getHomeEventsQuery;
    }

    @GetMapping
    List<OperationalEventResponse> events(@PathVariable UUID homeId) {
        return getHomeEventsQuery.execute(homeId).stream().map(OperationalEventResponse::from).toList();
    }
}
