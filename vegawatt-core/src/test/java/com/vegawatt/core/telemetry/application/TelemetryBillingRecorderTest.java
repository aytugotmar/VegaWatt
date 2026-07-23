package com.vegawatt.core.telemetry.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.anomaly.domain.AnomalyEvaluationResult;
import com.vegawatt.core.anomaly.domain.StandbyAnomalyEvaluationResult;
import com.vegawatt.core.billing.application.HomeUpdateOutcome;
import com.vegawatt.core.billing.domain.BillingAccount;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.billing.domain.QuotaTransition;
import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.events.OperationalEvent;
import com.vegawatt.core.common.events.OperationalEventRepository;
import com.vegawatt.core.common.events.OperationalEventType;
import com.vegawatt.core.common.time.BillingPeriodResolver;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceOperatingState;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.notification.domain.NotificationJob;
import com.vegawatt.core.notification.domain.NotificationJobRepository;
import com.vegawatt.core.telemetry.domain.ProcessedTelemetryEventRepository;
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
class TelemetryBillingRecorderTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final String PERIOD = BillingPeriodResolver.currentPeriod(NOW);
    private static final UUID HOME_ID = UUID.randomUUID();
    private static final UUID APPLIANCE_ID = UUID.randomUUID();
    private static final QuotaTransition NO_TRANSITION = new QuotaTransition(false, false);
    private static final int BREACH_THRESHOLD = 3;

    @Mock
    private BillingAccountRepository billingAccountRepository;
    @Mock
    private OperationalEventRepository operationalEventRepository;
    @Mock
    private ProcessedTelemetryEventRepository processedTelemetryEventRepository;
    @Mock
    private NotificationJobRepository notificationJobRepository;

    private TelemetryBillingRecorder recorder;
    private Home home;
    private Appliance appliance;
    private AnomalyEvaluationResult noAnomalyChange;
    private StandbyAnomalyEvaluationResult noStandbyChange;

    @BeforeEach
    void setUp() {
        recorder = new TelemetryBillingRecorder(billingAccountRepository, operationalEventRepository,
                processedTelemetryEventRepository, notificationJobRepository);
        home = Home.reconstitute(HOME_ID, "Test Ev", "test@example.com", new BigDecimal("500"),
                new BigDecimal("1000"), new BigDecimal("2.10"), new BigDecimal("3.50"), NOW, NOW, List.of());
        appliance = new Appliance(APPLIANCE_ID, HOME_ID, "Fridge", "REFRIGERATOR", new BigDecimal("200"),
                new BigDecimal("50"), new BigDecimal("150"), true, null, null, null, null, null);
        noAnomalyChange = new AnomalyEvaluationResult(0, false, false, false);
        noStandbyChange = StandbyAnomalyEvaluationResult.unchanged(0, 0, false);

        lenient().when(billingAccountRepository.findByHomeIdAndBillingPeriod(HOME_ID, PERIOD))
                .thenAnswer(invocation -> Optional.of(BillingAccount.open(HOME_ID, PERIOD, NOW)));
        lenient().when(operationalEventRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void marksEventAsProcessed() {
        UUID eventId = UUID.randomUUID();
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("0.002"), new BigDecimal("0.0042"),
                NO_TRANSITION, NO_TRANSITION);

        recorder.persist(eventId, home, outcome, appliance, noAnomalyChange, noStandbyChange,
                new BigDecimal("100"), null, false, NOW, NOW, BREACH_THRESHOLD);

        verify(processedTelemetryEventRepository).markProcessed(eventId, HOME_ID, APPLIANCE_ID, NOW);
    }

    @Test
    void createsQuota80NotificationJobAndMarksAccountNotifiedOnFirstCrossing() {
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("400"), new BigDecimal("840"),
                new QuotaTransition(true, false), NO_TRANSITION);

        recorder.persist(UUID.randomUUID(), home, outcome, appliance, noAnomalyChange, noStandbyChange,
                new BigDecimal("100"), null, false, NOW, NOW, BREACH_THRESHOLD);

        verify(operationalEventRepository).save(argThatEventType(OperationalEventType.ENERGY_QUOTA_80_REACHED));
        ArgumentCaptor<NotificationJob> jobCaptor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(notificationJobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().triggerType()).isEqualTo(AdvisoryTriggerType.QUOTA_80);
        assertThat(jobCaptor.getValue().triggerEventId()).isNotNull();

        ArgumentCaptor<BillingAccount> accountCaptor = ArgumentCaptor.forClass(BillingAccount.class);
        verify(billingAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().energyQuota80Notified()).isTrue();
    }

    @Test
    void doesNotDuplicateQuota80NotificationWhenAlreadyNotified() {
        BillingAccount alreadyNotified = BillingAccount.reconstitute(UUID.randomUUID(), HOME_ID, PERIOD,
                new BigDecimal("400").setScale(9), Money.zero(), false, true, false, false,
                false, 0L, NOW, NOW);
        when(billingAccountRepository.findByHomeIdAndBillingPeriod(HOME_ID, PERIOD))
                .thenReturn(Optional.of(alreadyNotified));
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("1"), new BigDecimal("2.1"),
                new QuotaTransition(true, false), NO_TRANSITION);

        recorder.persist(UUID.randomUUID(), home, outcome, appliance, noAnomalyChange, noStandbyChange,
                new BigDecimal("100"), null, false, NOW, NOW, BREACH_THRESHOLD);

        verify(operationalEventRepository, never()).save(any());
        verify(notificationJobRepository, never()).save(any());
    }

    @Test
    void suppresses80NotificationAndActivatesPenaltyWhenBudgetJumpsPastBothThresholds() {
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("500"), new BigDecimal("1050"),
                NO_TRANSITION, new QuotaTransition(true, true));

        recorder.persist(UUID.randomUUID(), home, outcome, appliance, noAnomalyChange, noStandbyChange,
                new BigDecimal("100"), null, false, NOW, NOW, BREACH_THRESHOLD);

        // 100% supersedes 80% for the same quota dimension within one event - only one job, not two.
        verify(operationalEventRepository).save(argThatEventType(OperationalEventType.BUDGET_QUOTA_100_REACHED));
        verify(operationalEventRepository, never()).save(argThatEventType(OperationalEventType.BUDGET_QUOTA_80_REACHED));
        verify(operationalEventRepository).save(argThatEventType(OperationalEventType.PENALTY_TARIFF_ACTIVATED));

        ArgumentCaptor<NotificationJob> jobCaptor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(notificationJobRepository, times(1)).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().triggerType()).isEqualTo(AdvisoryTriggerType.QUOTA_100);

        ArgumentCaptor<BillingAccount> accountCaptor = ArgumentCaptor.forClass(BillingAccount.class);
        verify(billingAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().penaltyActive()).isTrue();
        assertThat(accountCaptor.getValue().budgetQuota80Notified()).isTrue();
        assertThat(accountCaptor.getValue().budgetQuota100Notified()).isTrue();
    }

    @Test
    void createsAnomalyNotificationJobOnTransitionToAnomalous() {
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("1"), new BigDecimal("2.1"), NO_TRANSITION,
                NO_TRANSITION);
        AnomalyEvaluationResult transitioned = new AnomalyEvaluationResult(3, true, true, false);

        recorder.persist(UUID.randomUUID(), home, outcome, appliance, transitioned, noStandbyChange,
                new BigDecimal("100"), null, false, NOW, NOW, BREACH_THRESHOLD);

        verify(operationalEventRepository).save(argThatEventType(OperationalEventType.APPLIANCE_ANOMALY_DETECTED));
        ArgumentCaptor<NotificationJob> jobCaptor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(notificationJobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().triggerType()).isEqualTo(AdvisoryTriggerType.ANOMALY);
    }

    @Test
    void savesRecoveryEventWithoutCreatingNotificationJob() {
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("1"), new BigDecimal("2.1"), NO_TRANSITION,
                NO_TRANSITION);
        AnomalyEvaluationResult recovered = new AnomalyEvaluationResult(0, false, false, true);

        recorder.persist(UUID.randomUUID(), home, outcome, appliance, recovered, noStandbyChange,
                new BigDecimal("100"), null, false, NOW, NOW, BREACH_THRESHOLD);

        verify(operationalEventRepository).save(argThatEventType(OperationalEventType.APPLIANCE_ANOMALY_RECOVERED));
        verify(operationalEventRepository, times(1)).save(any());
        verify(notificationJobRepository, never()).save(any());
    }

    @Test
    void createsStandbyAnomalyNotificationJobOnTransitionToActiveWithFullMetadataInDetails() {
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("1"), new BigDecimal("2.1"), NO_TRANSITION,
                NO_TRANSITION);
        StandbyAnomalyEvaluationResult transitioned = new StandbyAnomalyEvaluationResult(3, 0, true, true, false,
                new BigDecimal("9"));

        recorder.persist(UUID.randomUUID(), home, outcome, appliance, noAnomalyChange, transitioned,
                new BigDecimal("12.5"), ApplianceOperatingState.STANDBY, false, NOW, NOW, BREACH_THRESHOLD);

        ArgumentCaptor<OperationalEvent> eventCaptor = ArgumentCaptor.forClass(OperationalEvent.class);
        verify(operationalEventRepository).save(eventCaptor.capture());
        OperationalEvent saved = eventCaptor.getValue();
        assertThat(saved.eventType()).isEqualTo(OperationalEventType.APPLIANCE_STANDBY_ANOMALY_DETECTED);
        assertThat(saved.details()).contains("operatingState=STANDBY", "observedPowerWatt=12.5",
                "expectedStandbyMaxWatt=" + appliance.standbyMaxWatt(), "calculatedThresholdWatt=9",
                "consecutiveMeasurementCount=3", "detectedAt=" + NOW);

        ArgumentCaptor<NotificationJob> jobCaptor = ArgumentCaptor.forClass(NotificationJob.class);
        verify(notificationJobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().triggerType()).isEqualTo(AdvisoryTriggerType.STANDBY_ANOMALY);
    }

    @Test
    void savesStandbyRecoveryEventWithoutCreatingNotificationJob() {
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("1"), new BigDecimal("2.1"), NO_TRANSITION,
                NO_TRANSITION);
        StandbyAnomalyEvaluationResult recovered = new StandbyAnomalyEvaluationResult(0, 3, false, false, true,
                new BigDecimal("9"));

        recorder.persist(UUID.randomUUID(), home, outcome, appliance, noAnomalyChange, recovered,
                new BigDecimal("1"), ApplianceOperatingState.STANDBY, false, NOW, NOW, BREACH_THRESHOLD);

        verify(operationalEventRepository)
                .save(argThatEventType(OperationalEventType.APPLIANCE_STANDBY_ANOMALY_RECOVERED));
        verify(operationalEventRepository, times(1)).save(any());
        verify(notificationJobRepository, never()).save(any());
    }

    @Test
    void savesTelemetryResumedEventWithoutCreatingNotificationJob() {
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("1"), new BigDecimal("2.1"), NO_TRANSITION,
                NO_TRANSITION);

        recorder.persist(UUID.randomUUID(), home, outcome, appliance, noAnomalyChange, noStandbyChange,
                new BigDecimal("100"), null, true, NOW, NOW, BREACH_THRESHOLD);

        verify(operationalEventRepository).save(argThatEventType(OperationalEventType.APPLIANCE_TELEMETRY_RESUMED));
        verify(operationalEventRepository, times(1)).save(any());
        verify(notificationJobRepository, never()).save(any());
    }

    @Test
    void doesNotSaveTelemetryResumedEventWhenNotTransitioned() {
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("1"), new BigDecimal("2.1"), NO_TRANSITION,
                NO_TRANSITION);

        recorder.persist(UUID.randomUUID(), home, outcome, appliance, noAnomalyChange, noStandbyChange,
                new BigDecimal("100"), null, false, NOW, NOW, BREACH_THRESHOLD);

        verify(operationalEventRepository, never())
                .save(argThatEventType(OperationalEventType.APPLIANCE_TELEMETRY_RESUMED));
    }

    private static OperationalEvent argThatEventType(OperationalEventType type) {
        return org.mockito.ArgumentMatchers.argThat(event -> event.eventType() == type);
    }
}
