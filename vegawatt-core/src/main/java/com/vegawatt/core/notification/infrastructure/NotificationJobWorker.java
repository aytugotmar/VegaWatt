package com.vegawatt.core.notification.infrastructure;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.notification.application.NotificationOrchestrator;
import com.vegawatt.core.notification.domain.NotificationJob;
import com.vegawatt.core.notification.domain.NotificationJobRepository;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationJobWorker {

    private static final int BATCH_SIZE = 50;

    private final NotificationJobRepository notificationJobRepository;
    private final NotificationOrchestrator notificationOrchestrator;
    private final ClockProvider clockProvider;
    private final Duration claimLease;

    public NotificationJobWorker(NotificationJobRepository notificationJobRepository,
                                  NotificationOrchestrator notificationOrchestrator, ClockProvider clockProvider,
                                  @Value("${vegawatt.notification.claim-lease-seconds:120}") long claimLeaseSeconds) {
        this.notificationJobRepository = notificationJobRepository;
        this.notificationOrchestrator = notificationOrchestrator;
        this.clockProvider = clockProvider;
        this.claimLease = Duration.ofSeconds(claimLeaseSeconds);
    }

    // The lease has to outlast one job's processing (a Gemini call plus an SMTP send), or a second
    // worker would reclaim a job still in flight and email the household twice. The default of two
    // minutes sits well above the few seconds processing actually takes. Claiming does not touch
    // attemptCount: a claim is not a failed attempt, so it must not eat into the retry budget.
    @Scheduled(fixedDelayString = "${vegawatt.notification.worker-interval-ms}",
            scheduler = "notificationTaskScheduler")
    public void processDueJobs() {
        var now = clockProvider.now();
        List<NotificationJob> claimedJobs = notificationJobRepository.claimDue(now, now.plus(claimLease), BATCH_SIZE);
        for (NotificationJob job : claimedJobs) {
            notificationOrchestrator.processJob(job);
        }
    }
}
