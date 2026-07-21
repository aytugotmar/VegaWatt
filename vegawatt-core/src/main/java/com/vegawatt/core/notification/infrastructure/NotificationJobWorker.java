package com.vegawatt.core.notification.infrastructure;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.notification.application.NotificationOrchestrator;
import com.vegawatt.core.notification.domain.NotificationJob;
import com.vegawatt.core.notification.domain.NotificationJobRepository;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationJobWorker {

    private static final int BATCH_SIZE = 50;

    private final NotificationJobRepository notificationJobRepository;
    private final NotificationOrchestrator notificationOrchestrator;
    private final ClockProvider clockProvider;

    public NotificationJobWorker(NotificationJobRepository notificationJobRepository,
                                  NotificationOrchestrator notificationOrchestrator, ClockProvider clockProvider) {
        this.notificationJobRepository = notificationJobRepository;
        this.notificationOrchestrator = notificationOrchestrator;
        this.clockProvider = clockProvider;
    }

    @Scheduled(fixedDelayString = "${vegawatt.notification.worker-interval-ms}")
    public void processDueJobs() {
        List<NotificationJob> dueJobs = notificationJobRepository.findDue(clockProvider.now(), BATCH_SIZE);
        for (NotificationJob job : dueJobs) {
            notificationOrchestrator.processJob(job);
        }
    }
}
