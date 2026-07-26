package com.vegawatt.core.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.ApplianceHealthStatus;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.notification.domain.AdvisoryContext;
import com.vegawatt.core.notification.domain.AdvisoryEmailDispatchException;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.notification.domain.AiRecommendation;
import com.vegawatt.core.notification.domain.NotificationJob;
import com.vegawatt.core.notification.domain.NotificationJobRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationOrchestratorTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID HOME_ID = UUID.randomUUID();

    @Mock
    private HomeRepository homeRepository;
    @Mock
    private HomeLiveStatePort homeLiveStatePort;
    @Mock
    private ApplianceLiveStatePort applianceLiveStatePort;
    @Mock
    private GenerateEnergyAdvisoryUseCase generateEnergyAdvisoryUseCase;
    @Mock
    private DispatchAdvisoryEmailUseCase dispatchAdvisoryEmailUseCase;
    @Mock
    private NotificationJobRepository notificationJobRepository;
    @Mock
    private ClockProvider clockProvider;

    private NotificationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        lenient().when(clockProvider.now()).thenReturn(NOW);
        orchestrator = new NotificationOrchestrator(homeRepository, homeLiveStatePort, applianceLiveStatePort,
                generateEnergyAdvisoryUseCase, dispatchAdvisoryEmailUseCase, notificationJobRepository, clockProvider);
    }

    private static NotificationJob pendingJob(int attemptCount) {
        NotificationJob fresh = NotificationJob.create(UUID.randomUUID(), HOME_ID, null, AdvisoryTriggerType.QUOTA_80, NOW);
        NotificationJob withAttempts = fresh;
        for (int i = 0; i < attemptCount; i++) {
            withAttempts = withAttempts.markFailedForRetry("previous failure", NOW);
        }
        return withAttempts;
    }

    @Test
    void marksJobSentOnSuccessfulProcessing() {
        Home home = Home.reconstitute(HOME_ID, "Test Ev", "test@example.com", new BigDecimal("500"),
                new BigDecimal("1000"), new BigDecimal("2.10"), new BigDecimal("3.50"), NOW, NOW, List.of());
        when(homeRepository.findById(HOME_ID)).thenReturn(Optional.of(home));
        when(homeLiveStatePort.get(HOME_ID)).thenReturn(Optional.of(HomeLiveState.zero(HOME_ID, "Test Ev", NOW)));
        when(applianceLiveStatePort.getByHomeId(HOME_ID)).thenReturn(List.of());
        when(generateEnergyAdvisoryUseCase.execute(any())).thenReturn(
                AiRecommendation.create(HOME_ID, AdvisoryTriggerType.QUOTA_80, "content", true, NOW,
                        UUID.randomUUID()));

        orchestrator.processJob(pendingJob(0));

        ArgumentCaptor<NotificationJob> captor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(notificationJobRepository).save(captor.capture());
        assertThat(captor.getValue().status().name()).isEqualTo("SENT");
        verify(dispatchAdvisoryEmailUseCase).execute(any(), org.mockito.ArgumentMatchers.eq("test@example.com"));
    }

    @Test
    void includesStandbyAnomalousAppliancesInTheAdvisoryContextAlongsideSafePowerAnomalous() {
        Home home = Home.reconstitute(HOME_ID, "Test Ev", "test@example.com", new BigDecimal("500"),
                new BigDecimal("1000"), new BigDecimal("2.10"), new BigDecimal("3.50"), NOW, NOW, List.of());
        UUID standbyAnomalousApplianceId = UUID.randomUUID();
        ApplianceLiveState standbyAnomalous = new ApplianceLiveState(HOME_ID, standbyAnomalousApplianceId, "TV",
                "TELEVISION", new BigDecimal("180"), new BigDecimal("12"), null, null, BigDecimal.ZERO.setScale(9),
                0, 0, false, 3, 0, true, ApplianceHealthStatus.NORMAL, NOW, null, 0L, 0L);
        when(homeRepository.findById(HOME_ID)).thenReturn(Optional.of(home));
        when(homeLiveStatePort.get(HOME_ID)).thenReturn(Optional.of(HomeLiveState.zero(HOME_ID, "Test Ev", NOW)));
        when(applianceLiveStatePort.getByHomeId(HOME_ID)).thenReturn(List.of(standbyAnomalous));
        when(generateEnergyAdvisoryUseCase.execute(any())).thenReturn(
                AiRecommendation.create(HOME_ID, AdvisoryTriggerType.STANDBY_ANOMALY, "content", true, NOW,
                        UUID.randomUUID()));

        orchestrator.processJob(NotificationJob.create(UUID.randomUUID(), HOME_ID, standbyAnomalousApplianceId,
                AdvisoryTriggerType.STANDBY_ANOMALY, NOW));

        ArgumentCaptor<AdvisoryContext> contextCaptor = ArgumentCaptor.forClass(AdvisoryContext.class);
        verify(generateEnergyAdvisoryUseCase).execute(contextCaptor.capture());
        assertThat(contextCaptor.getValue().anomalousApplianceNames())
                .containsExactly(standbyAnomalousApplianceId.toString());
    }

    @Test
    void schedulesRetryWithBackoffWhenHomeOrLiveStateMissing() {
        when(homeRepository.findById(HOME_ID)).thenReturn(Optional.empty());

        orchestrator.processJob(pendingJob(1));

        ArgumentCaptor<NotificationJob> captor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(notificationJobRepository).save(captor.capture());
        assertThat(captor.getValue().status().name()).isEqualTo("PENDING");
        assertThat(captor.getValue().attemptCount()).isEqualTo(2);
        assertThat(captor.getValue().nextAttemptAt()).isAfter(NOW);
        verify(generateEnergyAdvisoryUseCase, never()).execute(any());
    }

    @Test
    void keepsJobPendingForRetryWhenTheEmailFails() {
        Home home = Home.reconstitute(HOME_ID, "Test Ev", "test@example.com", new BigDecimal("500"),
                new BigDecimal("1000"), new BigDecimal("2.10"), new BigDecimal("3.50"), NOW, NOW, List.of());
        when(homeRepository.findById(HOME_ID)).thenReturn(Optional.of(home));
        when(homeLiveStatePort.get(HOME_ID)).thenReturn(Optional.of(HomeLiveState.zero(HOME_ID, "Test Ev", NOW)));
        when(applianceLiveStatePort.getByHomeId(HOME_ID)).thenReturn(List.of());
        when(generateEnergyAdvisoryUseCase.execute(any())).thenReturn(
                AiRecommendation.create(HOME_ID, AdvisoryTriggerType.QUOTA_80, "content", false, NOW,
                        UUID.randomUUID()));
        doThrow(new AdvisoryEmailDispatchException("smtp down", new RuntimeException()))
                .when(dispatchAdvisoryEmailUseCase).execute(any(), any());

        orchestrator.processJob(pendingJob(0));

        // Everything before the send succeeded, so it would be easy for this job to end up SENT.
        // A job whose email never left must stay claimable, or the notification is lost while
        // both records claim success.
        ArgumentCaptor<NotificationJob> captor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(notificationJobRepository).save(captor.capture());
        assertThat(captor.getValue().status().name()).isEqualTo("PENDING");
        assertThat(captor.getValue().attemptCount()).isEqualTo(1);
        assertThat(captor.getValue().nextAttemptAt()).isAfter(NOW);
        assertThat(captor.getValue().lastError()).contains("smtp down");
    }

    @Test
    void marksJobTerminallyFailedAfterMaxAttempts() {
        when(homeRepository.findById(HOME_ID)).thenReturn(Optional.empty());

        orchestrator.processJob(pendingJob(4));

        ArgumentCaptor<NotificationJob> captor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(notificationJobRepository).save(captor.capture());
        assertThat(captor.getValue().status().name()).isEqualTo("FAILED");
    }
}
