package com.vegawatt.core.notification.api;

import com.vegawatt.core.notification.application.GetHomeRecommendationsQuery;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/homes/{homeId}/recommendations")
class RecommendationController {

    private final GetHomeRecommendationsQuery getHomeRecommendationsQuery;

    RecommendationController(GetHomeRecommendationsQuery getHomeRecommendationsQuery) {
        this.getHomeRecommendationsQuery = getHomeRecommendationsQuery;
    }

    @GetMapping
    List<RecommendationResponse> recommendations(@PathVariable UUID homeId) {
        return getHomeRecommendationsQuery.execute(homeId).stream().map(RecommendationResponse::from).toList();
    }
}
