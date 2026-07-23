package com.vegawatt.core.common.events.api;

import com.vegawatt.core.access.domain.HomeAuthorizationService;
import com.vegawatt.core.common.events.application.GetHomeEventsQuery;
import com.vegawatt.core.common.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/homes/{homeId}/events")
class EventController {

    private final GetHomeEventsQuery getHomeEventsQuery;
    private final HomeAuthorizationService homeAuthorizationService;

    EventController(GetHomeEventsQuery getHomeEventsQuery, HomeAuthorizationService homeAuthorizationService) {
        this.getHomeEventsQuery = getHomeEventsQuery;
        this.homeAuthorizationService = homeAuthorizationService;
    }

    @GetMapping
    List<OperationalEventResponse> events(@PathVariable UUID homeId, @AuthenticationPrincipal CurrentUser currentUser) {
        homeAuthorizationService.requireAccess(currentUser, homeId);
        return getHomeEventsQuery.execute(homeId).stream().map(OperationalEventResponse::from).toList();
    }
}
