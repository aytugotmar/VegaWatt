package com.vegawatt.core.insight.api;

import com.vegawatt.core.common.security.CurrentUser;
import com.vegawatt.core.common.config.RateLimitProperties;
import com.vegawatt.core.common.rate.RateLimiter;
import com.vegawatt.core.insight.application.AskInsightUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/homes/{homeId}/insights")
class InsightController {

    private final AskInsightUseCase askInsightUseCase;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    InsightController(AskInsightUseCase askInsightUseCase, RateLimiter rateLimiter,
                       RateLimitProperties rateLimitProperties) {
        this.askInsightUseCase = askInsightUseCase;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
    }

    @PostMapping("/ask")
    ResponseEntity<AskInsightResponse> ask(
            @AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable UUID homeId,
            @Valid @RequestBody AskInsightRequest request,
            HttpServletRequest httpRequest) {

        String rateKey = "ask_insight:" + currentUser.userId() + ":" + homeId;
        rateLimiter.tryAcquire(rateKey, rateLimitProperties.insightPerMinute(), Duration.ofMinutes(1));

        AskInsightResponse response = askInsightUseCase.execute(currentUser, homeId, request);
        return ResponseEntity.ok(response);
    }
}
