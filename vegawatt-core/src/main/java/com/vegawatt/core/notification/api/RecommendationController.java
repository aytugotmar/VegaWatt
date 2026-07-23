package com.vegawatt.core.notification.api;

import com.vegawatt.core.access.domain.HomeAuthorizationService;
import com.vegawatt.core.common.security.CurrentUser;
import com.vegawatt.core.notification.application.GetHomeRecommendationsQuery;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/homes/{homeId}/recommendations")
class RecommendationController {

    private final GetHomeRecommendationsQuery getHomeRecommendationsQuery;
    private final HomeAuthorizationService homeAuthorizationService;

    RecommendationController(GetHomeRecommendationsQuery getHomeRecommendationsQuery,
                              HomeAuthorizationService homeAuthorizationService) {
        this.getHomeRecommendationsQuery = getHomeRecommendationsQuery;
        this.homeAuthorizationService = homeAuthorizationService;
    }

    @GetMapping
    List<RecommendationResponse> recommendations(@PathVariable UUID homeId, @AuthenticationPrincipal CurrentUser currentUser) {
        homeAuthorizationService.requireAccess(currentUser, homeId);
        return getHomeRecommendationsQuery.execute(homeId).stream().map(RecommendationResponse::from).toList();
    }
}
