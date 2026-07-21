package com.vegawatt.core.notification.application;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.notification.domain.AdvisoryContext;
import com.vegawatt.core.notification.domain.AiRecommendation;
import com.vegawatt.core.notification.domain.NotificationJob;
import com.vegawatt.core.notification.domain.NotificationJobRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Processes one durable {@link NotificationJob} at a time, invoked by {@code
 * NotificationJobWorker} (never directly from the telemetry-processing thread, so a slow Gemini
 * call or SMTP send can never block the Kafka consumer). On failure the job is retried with
 * backoff, up to {@link #MAX_ATTEMPTS}, instead of the notification being silently lost.
 */
@Service
public class NotificationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NotificationOrchestrator.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final long MAX_BACKOFF_SECONDS = 60;

    private final HomeRepository homeRepository;
    private final HomeLiveStatePort homeLiveStatePort;
    private final ApplianceLiveStatePort applianceLiveStatePort;
    private final GenerateEnergyAdvisoryUseCase generateEnergyAdvisoryUseCase;
    private final DispatchAdvisoryEmailUseCase dispatchAdvisoryEmailUseCase;
    private final NotificationJobRepository notificationJobRepository;
    private final ClockProvider clockProvider;

    public NotificationOrchestrator(HomeRepository homeRepository, HomeLiveStatePort homeLiveStatePort,
                                     ApplianceLiveStatePort applianceLiveStatePort,
                                     GenerateEnergyAdvisoryUseCase generateEnergyAdvisoryUseCase,
                                     DispatchAdvisoryEmailUseCase dispatchAdvisoryEmailUseCase,
                                     NotificationJobRepository notificationJobRepository, ClockProvider clockProvider) {
        this.homeRepository = homeRepository;
        this.homeLiveStatePort = homeLiveStatePort;
        this.applianceLiveStatePort = applianceLiveStatePort;
        this.generateEnergyAdvisoryUseCase = generateEnergyAdvisoryUseCase;
        this.dispatchAdvisoryEmailUseCase = dispatchAdvisoryEmailUseCase;
        this.notificationJobRepository = notificationJobRepository;
        this.clockProvider = clockProvider;
    }

    public void processJob(NotificationJob job) {
        try {
            UUID homeId = job.homeId();
            Home home = homeRepository.findById(homeId).orElse(null);
            HomeLiveState liveState = homeLiveStatePort.get(homeId).orElse(null);
            if (home == null || liveState == null) {
                throw new IllegalStateException("Home or live state not found for home " + homeId);
            }

            List<String> anomalousApplianceNames = applianceLiveStatePort.getByHomeId(homeId).stream()
                    .filter(ApplianceLiveState::anomalous)
                    .map(state -> applianceName(home, state.applianceId()))
                    .toList();

            AdvisoryContext context = new AdvisoryContext(homeId, home.name(), job.triggerType(),
                    liveState.energyQuotaPercentage(), liveState.budgetQuotaPercentage(), liveState.currentCost(),
                    liveState.tariffState(), anomalousApplianceNames, job.triggerEventId());

            AiRecommendation recommendation = generateEnergyAdvisoryUseCase.execute(context);
            dispatchAdvisoryEmailUseCase.execute(recommendation, home.contactEmail());

            notificationJobRepository.save(job.markSent());
        } catch (RuntimeException e) {
            handleFailure(job, e);
        }
    }

    private void handleFailure(NotificationJob job, RuntimeException e) {
        int nextAttemptNumber = job.attemptCount() + 1;
        if (nextAttemptNumber >= MAX_ATTEMPTS) {
            log.error("Notification job {} for home {} failed permanently after {} attempts", job.id(), job.homeId(),
                    nextAttemptNumber, e);
            notificationJobRepository.save(job.markTerminallyFailed(e.getMessage()));
            return;
        }

        Instant nextAttemptAt = clockProvider.now().plusSeconds(backoffSeconds(nextAttemptNumber));
        log.warn("Notification job {} for home {} failed (attempt {}/{}), retrying at {}", job.id(), job.homeId(),
                nextAttemptNumber, MAX_ATTEMPTS, nextAttemptAt, e);
        notificationJobRepository.save(job.markFailedForRetry(e.getMessage(), nextAttemptAt));
    }

    private static long backoffSeconds(int attemptNumber) {
        return Math.min(MAX_BACKOFF_SECONDS, 1L << attemptNumber);
    }

    private static String applianceName(Home home, UUID applianceId) {
        return home.appliances().stream()
                .filter(appliance -> appliance.id().equals(applianceId))
                .map(Appliance::name)
                .findFirst()
                .orElse(applianceId.toString());
    }
}
