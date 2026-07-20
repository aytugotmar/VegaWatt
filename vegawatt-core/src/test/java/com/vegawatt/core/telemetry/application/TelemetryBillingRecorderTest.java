package com.vegawatt.core.telemetry.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.anomaly.domain.AnomalyEvaluationResult;
import com.vegawatt.core.billing.domain.BillingAccount;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.billing.domain.QuotaTransition;
import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.events.OperationalEvent;
import com.vegawatt.core.common.events.OperationalEventRepository;
import com.vegawatt.core.common.events.OperationalEventType;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
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
    private static final UUID HOME_ID = UUID.randomUUID();
    private static final UUID APPLIANCE_ID = UUID.randomUUID();
    private static final QuotaTransition NO_TRANSITION = new QuotaTransition(false, false);

    @Mock
    private BillingAccountRepository billingAccountRepository;
    @Mock
    private OperationalEventRepository operationalEventRepository;
    @Mock
    private ProcessedTelemetryEventRepository processedTelemetryEventRepository;

    private TelemetryBillingRecorder recorder;
    private Home home;
    private Appliance appliance;
    private AnomalyEvaluationResult noAnomalyChange;

    @BeforeEach
    void setUp() {
        recorder = new TelemetryBillingRecorder(billingAccountRepository, operationalEventRepository,
                processedTelemetryEventRepository);
        home = Home.reconstitute(HOME_ID, "Test Ev", "test@example.com", new BigDecimal("500"),
                new BigDecimal("1000"), new BigDecimal("2.10"), new BigDecimal("3.50"), NOW, NOW, List.of());
        appliance = new Appliance(APPLIANCE_ID, HOME_ID, "Fridge", "REFRIGERATOR", new BigDecimal("200"),
                new BigDecimal("50"), new BigDecimal("150"), true);
        noAnomalyChange = new AnomalyEvaluationResult(0, false, false, false);

        lenient().when(billingAccountRepository.findByHomeId(HOME_ID))
                .thenAnswer(invocation -> Optional.of(BillingAccount.open(HOME_ID, "2026-01", NOW)));
    }

    @Test
    void marksEventAsProcessed() {
        UUID eventId = UUID.randomUUID();
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("0.002"), new BigDecimal("0.0042"),
                NO_TRANSITION, NO_TRANSITION);

        recorder.persist(eventId, home, outcome, appliance, noAnomalyChange, NOW);

        verify(processedTelemetryEventRepository).markProcessed(eventId, HOME_ID, APPLIANCE_ID, NOW);
    }

    @Test
    void firesQuota80TriggerAndMarksAccountNotifiedOnFirstCrossing() {
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("400"), new BigDecimal("840"),
                new QuotaTransition(true, false), NO_TRANSITION);

        List<AdvisoryTriggerType> triggers = recorder.persist(UUID.randomUUID(), home, outcome, appliance,
                noAnomalyChange, NOW);

        assertThat(triggers).containsExactly(AdvisoryTriggerType.QUOTA_80);
        verify(operationalEventRepository).save(argThatEventType(OperationalEventType.QUOTA_80_REACHED));

        ArgumentCaptor<BillingAccount> captor = ArgumentCaptor.forClass(BillingAccount.class);
        verify(billingAccountRepository).save(captor.capture());
        assertThat(captor.getValue().energyQuota80Notified()).isTrue();
    }

    @Test
    void doesNotDuplicateQuota80NotificationWhenAlreadyNotified() {
        BillingAccount alreadyNotified = BillingAccount.reconstitute(UUID.randomUUID(), HOME_ID, "2026-01",
                new BigDecimal("400").setScale(9), Money.zero(), false, true, false, false,
                false, 0L, NOW, NOW);
        when(billingAccountRepository.findByHomeId(HOME_ID)).thenReturn(Optional.of(alreadyNotified));
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("1"), new BigDecimal("2.1"),
                new QuotaTransition(true, false), NO_TRANSITION);

        List<AdvisoryTriggerType> triggers = recorder.persist(UUID.randomUUID(), home, outcome, appliance,
                noAnomalyChange, NOW);

        assertThat(triggers).isEmpty();
        verify(operationalEventRepository, never()).save(any());
    }

    @Test
    void activatesPenaltyAndFiresQuota100TriggerOnBudgetCrossing() {
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("500"), new BigDecimal("1050"),
                NO_TRANSITION, new QuotaTransition(true, true));

        List<AdvisoryTriggerType> triggers = recorder.persist(UUID.randomUUID(), home, outcome, appliance,
                noAnomalyChange, NOW);

        assertThat(triggers).contains(AdvisoryTriggerType.QUOTA_80, AdvisoryTriggerType.QUOTA_100);
        verify(operationalEventRepository).save(argThatEventType(OperationalEventType.PENALTY_TARIFF_ACTIVATED));

        ArgumentCaptor<BillingAccount> captor = ArgumentCaptor.forClass(BillingAccount.class);
        verify(billingAccountRepository).save(captor.capture());
        assertThat(captor.getValue().penaltyActive()).isTrue();
    }

    @Test
    void firesAnomalyTriggerOnTransitionToAnomalous() {
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("1"), new BigDecimal("2.1"), NO_TRANSITION,
                NO_TRANSITION);
        AnomalyEvaluationResult transitioned = new AnomalyEvaluationResult(3, true, true, false);

        List<AdvisoryTriggerType> triggers = recorder.persist(UUID.randomUUID(), home, outcome, appliance,
                transitioned, NOW);

        assertThat(triggers).containsExactly(AdvisoryTriggerType.ANOMALY);
        verify(operationalEventRepository).save(argThatEventType(OperationalEventType.APPLIANCE_ANOMALY_DETECTED));
    }

    @Test
    void savesRecoveryEventWithoutTriggeringAdvisory() {
        HomeUpdateOutcome outcome = new HomeUpdateOutcome(new BigDecimal("1"), new BigDecimal("2.1"), NO_TRANSITION,
                NO_TRANSITION);
        AnomalyEvaluationResult recovered = new AnomalyEvaluationResult(0, false, false, true);

        List<AdvisoryTriggerType> triggers = recorder.persist(UUID.randomUUID(), home, outcome, appliance, recovered,
                NOW);

        assertThat(triggers).isEmpty();
        verify(operationalEventRepository).save(argThatEventType(OperationalEventType.APPLIANCE_ANOMALY_RECOVERED));
        verify(operationalEventRepository, times(1)).save(any());
    }

    private static OperationalEvent argThatEventType(OperationalEventType type) {
        return org.mockito.ArgumentMatchers.argThat(event -> event.eventType() == type);
    }
}
