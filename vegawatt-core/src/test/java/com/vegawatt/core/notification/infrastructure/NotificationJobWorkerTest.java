package com.vegawatt.core.notification.infrastructure;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.notification.application.NotificationOrchestrator;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.notification.domain.NotificationJob;
import com.vegawatt.core.notification.domain.NotificationJobRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationJobWorkerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private NotificationJobRepository notificationJobRepository;
    @Mock
    private NotificationOrchestrator notificationOrchestrator;
    @Mock
    private ClockProvider clockProvider;

    @Test
    void processesEveryDueJob() {
        when(clockProvider.now()).thenReturn(NOW);
        NotificationJob jobOne = NotificationJob.create(UUID.randomUUID(), UUID.randomUUID(),
                AdvisoryTriggerType.QUOTA_80, NOW);
        NotificationJob jobTwo = NotificationJob.create(UUID.randomUUID(), UUID.randomUUID(),
                AdvisoryTriggerType.ANOMALY, NOW);
        when(notificationJobRepository.findDue(NOW, 50)).thenReturn(List.of(jobOne, jobTwo));

        NotificationJobWorker worker = new NotificationJobWorker(notificationJobRepository, notificationOrchestrator,
                clockProvider);
        worker.processDueJobs();

        verify(notificationOrchestrator).processJob(jobOne);
        verify(notificationOrchestrator).processJob(jobTwo);
    }
}
