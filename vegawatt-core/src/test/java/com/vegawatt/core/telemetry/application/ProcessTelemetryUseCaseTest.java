package com.vegawatt.core.telemetry.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vegawatt.core.anomaly.application.EvaluateApplianceAnomalyUseCase;
import com.vegawatt.core.anomaly.application.EvaluateStandbyConsumptionUseCase;
import com.vegawatt.core.appliancecatalog.domain.ApplianceBehaviorProfile;
import com.vegawatt.core.billing.application.EvaluateHomeBillingUseCase;
import com.vegawatt.core.billing.domain.BillingAccount;
import com.vegawatt.core.billing.domain.BillingAccountRepository;
import com.vegawatt.core.common.ApplianceHealthStatus;
import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import com.vegawatt.core.common.config.AnomalyProperties;
import com.vegawatt.core.common.config.StandbyAnomalyProperties;
import com.vegawatt.core.common.time.BillingPeriodResolver;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.ApplianceOperatingState;
import com.vegawatt.core.home.domain.ApplianceRepository;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.home.domain.TelemetryLiveStatePort;
import com.vegawatt.core.home.domain.TelemetryLiveStateUpdate;
import com.vegawatt.core.telemetry.domain.InvalidTelemetryReadingException;
import com.vegawatt.core.telemetry.domain.ProcessedTelemetryEventRepository;
import com.vegawatt.core.telemetry.domain.TelemetryReading;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessTelemetryUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID HOME_ID = UUID.randomUUID();
    private static final UUID APPLIANCE_ID = UUID.randomUUID();

    @Mock
    private HomeRepository homeRepository;
    @Mock
    private ApplianceRepository applianceRepository;
    @Mock
    private HomeLiveStatePort homeLiveStatePort;
    @Mock
    private ApplianceLiveStatePort applianceLiveStatePort;
    @Mock
    private TelemetryLiveStatePort telemetryLiveStatePort;
    @Mock
    private BillingAccountRepository billingAccountRepository;
    @Mock
    private ProcessedTelemetryEventRepository processedTelemetryEventRepository;
    @Mock
    private TelemetryBillingRecorder telemetryBillingRecorder;
    @Mock
    private ClockProvider clockProvider;

    private ProcessTelemetryUseCase useCase;
    private HomeLiveState homeLiveStateSlot;
    private ApplianceLiveState applianceLiveStateSlot;

    @BeforeEach
    void setUp() {
        EvaluateHomeBillingUseCase evaluateHomeBillingUseCase = new EvaluateHomeBillingUseCase(
                billingAccountRepository);
        EvaluateApplianceAnomalyUseCase evaluateApplianceAnomalyUseCase = new EvaluateApplianceAnomalyUseCase(
                new AnomalyProperties(3, 3, new BigDecimal("0.90")));
        EvaluateStandbyConsumptionUseCase evaluateStandbyConsumptionUseCase = new EvaluateStandbyConsumptionUseCase(
                new StandbyAnomalyProperties(new BigDecimal("3"), new BigDecimal("2"), 3, 3));

        useCase = new ProcessTelemetryUseCase(homeRepository, applianceRepository, homeLiveStatePort,
                applianceLiveStatePort, telemetryLiveStatePort, evaluateHomeBillingUseCase,
                evaluateApplianceAnomalyUseCase, evaluateStandbyConsumptionUseCase, processedTelemetryEventRepository,
                telemetryBillingRecorder, clockProvider);

        lenient().when(clockProvider.now()).thenReturn(NOW);
        lenient().when(homeRepository.findById(HOME_ID)).thenReturn(Optional.of(testHome()));
        lenient().when(billingAccountRepository.findByHomeIdAndBillingPeriod(HOME_ID, BillingPeriodResolver.currentPeriod(NOW)))
                .thenReturn(Optional.empty());
        lenient().when(applianceRepository.findById(APPLIANCE_ID)).thenReturn(Optional.of(testAppliance(true)));
        lenient().when(telemetryLiveStatePort.update(eq(HOME_ID), eq(APPLIANCE_ID), any(), any()))
                .thenAnswer(invocation -> {
                    UnaryOperator<HomeLiveState> homeMutator = invocation.getArgument(2);
                    UnaryOperator<ApplianceLiveState> applianceMutator = invocation.getArgument(3);
                    homeLiveStateSlot = homeMutator.apply(homeLiveStateSlot);
                    applianceLiveStateSlot = applianceMutator.apply(applianceLiveStateSlot);
                    return new TelemetryLiveStateUpdate(homeLiveStateSlot, applianceLiveStateSlot);
                });
    }

    private static Home testHome() {
        return Home.reconstitute(HOME_ID, "Test Ev", "test@example.com", new BigDecimal("500"),
                new BigDecimal("1000"), new BigDecimal("2.10"), new BigDecimal("3.50"), NOW, NOW, List.of());
    }

    private static Appliance testAppliance(boolean active) {
        return new Appliance(APPLIANCE_ID, HOME_ID, "Fridge", "REFRIGERATOR", new BigDecimal("200"),
                new BigDecimal("50"), new BigDecimal("150"), active, null, null, null, null, null);
    }

    private static Appliance standbyEligibleAppliance() {
        return new Appliance(APPLIANCE_ID, HOME_ID, "TV", "TELEVISION", new BigDecimal("180"),
                new BigDecimal("40"), new BigDecimal("150"), true, null, null,
                ApplianceBehaviorProfile.STANDBY_DEVICE, new BigDecimal("0.5"), new BigDecimal("3"));
    }

    private static TelemetryReading reading(UUID eventId, BigDecimal powerWatt) {
        return reading(eventId, 1L, powerWatt);
    }

    private static TelemetryReading reading(UUID eventId, long sequenceNumber, BigDecimal powerWatt) {
        return new TelemetryReading(eventId, HOME_ID, APPLIANCE_ID, sequenceNumber, powerWatt, null, null, 5, NOW);
    }

    private static ApplianceLiveState applianceStateWithSequence(long lastProcessedSequence) {
        return new ApplianceLiveState(HOME_ID, APPLIANCE_ID, "Fridge", "REFRIGERATOR", new BigDecimal("200"),
                new BigDecimal("0"), null, null, BigDecimal.ZERO.setScale(9), 0, 0, false, 0, 0, false,
                ApplianceHealthStatus.NORMAL, NOW, null, lastProcessedSequence, 0L);
    }

    @Test
    void skipsAlreadyProcessedEvent() {
        UUID eventId = UUID.randomUUID();
        when(processedTelemetryEventRepository.existsByEventId(eventId)).thenReturn(true);

        useCase.execute(reading(eventId, new BigDecimal("1000")));

        verifyNoInteractions(homeLiveStatePort, telemetryLiveStatePort, telemetryBillingRecorder);
    }

    @Test
    void rejectsTelemetryWhenApplianceDoesNotBelongToHome() {
        UUID otherHomeId = UUID.randomUUID();
        TelemetryReading mismatched = new TelemetryReading(UUID.randomUUID(), otherHomeId, APPLIANCE_ID, 1L,
                new BigDecimal("1000"), null, null, 5, NOW);
        when(homeRepository.findById(otherHomeId)).thenReturn(Optional.of(
                Home.reconstitute(otherHomeId, "Other Ev", "other@example.com", new BigDecimal("500"),
                        new BigDecimal("1000"), new BigDecimal("2.10"), new BigDecimal("3.50"), NOW, NOW, List.of())));

        assertThatThrownBy(() -> useCase.execute(mismatched))
                .isInstanceOf(InvalidTelemetryReadingException.class);

        verifyNoInteractions(homeLiveStatePort, telemetryLiveStatePort, telemetryBillingRecorder);
    }

    @Test
    void skipsInactiveApplianceWithoutTouchingLiveState() {
        when(applianceRepository.findById(APPLIANCE_ID)).thenReturn(Optional.of(testAppliance(false)));

        useCase.execute(reading(UUID.randomUUID(), new BigDecimal("1000")));

        verifyNoInteractions(homeLiveStatePort, telemetryLiveStatePort, telemetryBillingRecorder);
    }

    @Test
    void persistsBillingAfterLiveStateUpdate() {
        useCase.execute(reading(UUID.randomUUID(), new BigDecimal("1000")));

        verify(telemetryBillingRecorder).persist(any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), any(), anyInt());
    }

    @Test
    void propagatesOperatingStateAndModeIntoApplianceLiveState() {
        TelemetryReading reading = new TelemetryReading(UUID.randomUUID(), HOME_ID, APPLIANCE_ID, 1L,
                new BigDecimal("1000"), ApplianceOperatingState.STANDBY, "STANDBY", 5, NOW);

        useCase.execute(reading);

        assertThat(applianceLiveStateSlot.operatingState()).isEqualTo(ApplianceOperatingState.STANDBY);
        assertThat(applianceLiveStateSlot.operatingMode()).isEqualTo("STANDBY");
    }

    @Test
    void v1TelemetryWithoutOperatingStateNeverTouchesStandbyFieldsAndSafePowerFlowStillWorks() {
        when(applianceRepository.findById(APPLIANCE_ID)).thenReturn(Optional.of(standbyEligibleAppliance()));
        TelemetryReading v1Reading = new TelemetryReading(UUID.randomUUID(), HOME_ID, APPLIANCE_ID, 1L,
                new BigDecimal("1000"), null, null, 5, NOW);

        useCase.execute(v1Reading);

        assertThat(applianceLiveStateSlot.standbyAnomalyActive()).isFalse();
        assertThat(applianceLiveStateSlot.standbyBreachCount()).isZero();
        assertThat(applianceLiveStateSlot.consecutiveBreachCount()).isEqualTo(1);
        verify(telemetryBillingRecorder).persist(any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), any(), anyInt());
    }

    @Test
    void threeConsecutiveHighStandbyReadingsActivateStandbyAnomaly() {
        when(applianceRepository.findById(APPLIANCE_ID)).thenReturn(Optional.of(standbyEligibleAppliance()));

        useCase.execute(new TelemetryReading(UUID.randomUUID(), HOME_ID, APPLIANCE_ID, 1L, new BigDecimal("20"),
                ApplianceOperatingState.STANDBY, "STANDBY", 5, NOW));
        assertThat(applianceLiveStateSlot.standbyAnomalyActive()).isFalse();

        useCase.execute(new TelemetryReading(UUID.randomUUID(), HOME_ID, APPLIANCE_ID, 1L, new BigDecimal("20"),
                ApplianceOperatingState.STANDBY, "STANDBY", 5, NOW));
        assertThat(applianceLiveStateSlot.standbyAnomalyActive()).isFalse();

        useCase.execute(new TelemetryReading(UUID.randomUUID(), HOME_ID, APPLIANCE_ID, 1L, new BigDecimal("20"),
                ApplianceOperatingState.STANDBY, "STANDBY", 5, NOW));

        assertThat(applianceLiveStateSlot.standbyAnomalyActive()).isTrue();
        assertThat(applianceLiveStateSlot.standbyBreachCount()).isEqualTo(3);
    }

    @Test
    void safePowerAndStandbyAnomaliesAreIndependent() {
        when(applianceRepository.findById(APPLIANCE_ID)).thenReturn(Optional.of(standbyEligibleAppliance()));

        for (int i = 0; i < 3; i++) {
            useCase.execute(new TelemetryReading(UUID.randomUUID(), HOME_ID, APPLIANCE_ID, 1L, new BigDecimal("20"),
                    ApplianceOperatingState.STANDBY, "STANDBY", 5, NOW));
        }
        assertThat(applianceLiveStateSlot.standbyAnomalyActive()).isTrue();
        assertThat(applianceLiveStateSlot.anomalous()).isFalse();

        for (int i = 0; i < 3; i++) {
            useCase.execute(new TelemetryReading(UUID.randomUUID(), HOME_ID, APPLIANCE_ID, 1L, new BigDecimal("1000"),
                    ApplianceOperatingState.ACTIVE, "IN_USE", 5, NOW));
        }
        assertThat(applianceLiveStateSlot.anomalous()).isTrue();
        assertThat(applianceLiveStateSlot.standbyAnomalyActive())
                .as("safe-power breach evaluation must never touch the independent standby anomaly state")
                .isTrue();
    }

    @Test
    void telemetryResumesAfterStaleStatusResetsHealthAndFlagsRecorderForResumedEvent() {
        applianceLiveStateSlot = new ApplianceLiveState(HOME_ID, APPLIANCE_ID, "Fridge", "REFRIGERATOR",
                new BigDecimal("200"), new BigDecimal("0"), null, null, BigDecimal.ZERO.setScale(9), 0, 0, false, 0,
                0, false, ApplianceHealthStatus.STALE, NOW.minusSeconds(60), null, 0L, 0L);

        useCase.execute(reading(UUID.randomUUID(), new BigDecimal("1000")));

        assertThat(applianceLiveStateSlot.telemetryHealthStatus()).isEqualTo(ApplianceHealthStatus.NORMAL);
        verify(telemetryBillingRecorder).persist(any(), any(), any(), any(), any(), any(), any(), any(), eq(true),
                any(), any(), anyInt());
    }

    @Test
    void doesNotFlagResumedWhenAlreadyNormal() {
        useCase.execute(reading(UUID.randomUUID(), new BigDecimal("1000")));

        assertThat(applianceLiveStateSlot.telemetryHealthStatus()).isEqualTo(ApplianceHealthStatus.NORMAL);
        verify(telemetryBillingRecorder).persist(any(), any(), any(), any(), any(), any(), any(), any(), eq(false),
                any(), any(), anyInt());
    }

    @Test
    void recoversAccumulatedStateFromBillingAccountAfterIgniteCacheMiss() {
        BillingAccount billingAccount = BillingAccount.reconstitute(UUID.randomUUID(), HOME_ID, "2026-01",
                new BigDecimal("400.5").setScale(9), Money.of(new BigDecimal("900.00")), true, true, false, true,
                false, 0L, NOW, NOW);
        when(billingAccountRepository.findByHomeIdAndBillingPeriod(HOME_ID, BillingPeriodResolver.currentPeriod(NOW)))
                .thenReturn(Optional.of(billingAccount));

        useCase.execute(reading(UUID.randomUUID(), new BigDecimal("1000")));

        assertThat(homeLiveStateSlot.currentEnergyKwh()).isGreaterThan(new BigDecimal("400.5"));
        assertThat(homeLiveStateSlot.currentCost().amount()).isGreaterThan(new BigDecimal("900.00"));
        assertThat(homeLiveStateSlot.tariffState()).isEqualTo(TariffState.PENALTY);
    }

    @Test
    void accumulatesSubKurusCostAcrossManySmallTelemetryEvents() {
        for (int i = 0; i < 500; i++) {
            useCase.execute(reading(UUID.randomUUID(), new BigDecimal("1450.5")));
        }

        assertThat(homeLiveStateSlot.currentCost().rounded()).isNotEqualByComparingTo("0.00");
    }

    @Test
    void compensatesBothHomeAndApplianceStateWhenPersistFails() {
        doThrow(new RuntimeException("postgres unavailable"))
                .when(telemetryBillingRecorder).persist(any(), any(), any(), any(), any(), any(), any(), any(),
                        anyBoolean(), any(), any(), anyInt());

        UUID eventId = UUID.randomUUID();
        assertThatThrownBy(() -> useCase.execute(reading(eventId, new BigDecimal("1000"))))
                .isInstanceOf(RuntimeException.class);

        verify(telemetryLiveStatePort).restore(eq(HOME_ID), eq(APPLIANCE_ID), eq(eventId), anyLong(), anyLong(), any(), any());
    }

    @Test
    void compensatesUsingTheExactVersionsStampedByThisEventsOwnUpdate() {
        // Distinct from the compare-and-swap correctness proven in IgniteTelemetryLiveStateAdapterTest
        // (which tests restore() in isolation): this proves ProcessTelemetryUseCase actually threads
        // the real stateVersion values from its own update() result through to restore(), rather than
        // some placeholder — the wiring a previous eventId-keyed CAS didn't need at all.
        homeLiveStateSlot = new HomeLiveState(HOME_ID, "Test Ev", BigDecimal.ZERO.setScale(9), Money.zero(),
                BigDecimal.ZERO, BigDecimal.ZERO, TariffState.BASE, false, BillingPeriodResolver.currentPeriod(NOW),
                NOW, null, 7L);
        applianceLiveStateSlot = applianceStateWithSequence(0L).withStateVersion(9L);
        doThrow(new RuntimeException("postgres unavailable"))
                .when(telemetryBillingRecorder).persist(any(), any(), any(), any(), any(), any(), any(), any(),
                        anyBoolean(), any(), any(), anyInt());

        UUID eventId = UUID.randomUUID();
        assertThatThrownBy(() -> useCase.execute(reading(eventId, new BigDecimal("1000"))))
                .isInstanceOf(RuntimeException.class);

        org.mockito.ArgumentCaptor<Long> homeVersion = org.mockito.ArgumentCaptor.forClass(Long.class);
        org.mockito.ArgumentCaptor<Long> applianceVersion = org.mockito.ArgumentCaptor.forClass(Long.class);
        verify(telemetryLiveStatePort).restore(eq(HOME_ID), eq(APPLIANCE_ID), eq(eventId), homeVersion.capture(),
                applianceVersion.capture(), any(), any());
        assertThat(homeVersion.getValue()).isEqualTo(7L);
        assertThat(applianceVersion.getValue()).isEqualTo(9L);
    }

    @Test
    void handlesConcurrentDuplicateEventGracefully() {
        doThrow(processedTelemetryEventPrimaryKeyViolation())
                .when(telemetryBillingRecorder).persist(any(), any(), any(), any(), any(), any(), any(), any(),
                        anyBoolean(), any(), any(), anyInt());

        // Should not throw, but restore Ignite state and skip the duplicate gracefully.
        UUID eventId = UUID.randomUUID();
        useCase.execute(reading(eventId, new BigDecimal("1000")));

        verify(telemetryLiveStatePort).restore(eq(HOME_ID), eq(APPLIANCE_ID), eq(eventId), anyLong(), anyLong(), any(), any());
    }

    @Test
    void compensatesStateAndRethrowsANonDuplicateIntegrityViolationInsteadOfSwallowingIt() {
        // A foreign-key violation, a not-null failure, a corrupt billing row, ... none of these are
        // a harmless double-delivery, and swallowing them would silently drop the telemetry event
        // forever (offset still commits, no retry, no DLT). Only the processed_telemetry_events
        // primary-key collision is a duplicate; everything else must propagate.
        org.hibernate.exception.ConstraintViolationException foreignKeyViolation =
                new org.hibernate.exception.ConstraintViolationException("insert failed",
                        new java.sql.SQLException("violates foreign key constraint", "23503"),
                        "fk_appliance_live_state_home_id");
        org.springframework.dao.DataIntegrityViolationException notADuplicate =
                new org.springframework.dao.DataIntegrityViolationException("insert failed", foreignKeyViolation);
        doThrow(notADuplicate)
                .when(telemetryBillingRecorder).persist(any(), any(), any(), any(), any(), any(), any(), any(),
                        anyBoolean(), any(), any(), anyInt());

        UUID eventId = UUID.randomUUID();
        assertThatThrownBy(() -> useCase.execute(reading(eventId, new BigDecimal("1000"))))
                .isSameAs(notADuplicate);

        verify(telemetryLiveStatePort).restore(eq(HOME_ID), eq(APPLIANCE_ID), eq(eventId), anyLong(), anyLong(), any(), any());
    }

    @Test
    void dropsOutOfOrderEventWhoseSequenceIsNotNewerThanTheLastApplied() {
        when(applianceLiveStatePort.get(HOME_ID, APPLIANCE_ID))
                .thenReturn(Optional.of(applianceStateWithSequence(100L)));

        useCase.execute(reading(UUID.randomUUID(), 50L, new BigDecimal("1000")));

        // A stale event must not touch live state, the billing period, or the anomaly counters.
        verifyNoInteractions(telemetryLiveStatePort, telemetryBillingRecorder);
    }

    @Test
    void processesEventWhoseSequenceIsNewerAndAdvancesTheHighWaterMark() {
        applianceLiveStateSlot = applianceStateWithSequence(50L);
        when(applianceLiveStatePort.get(HOME_ID, APPLIANCE_ID)).thenReturn(Optional.of(applianceLiveStateSlot));

        useCase.execute(reading(UUID.randomUUID(), 100L, new BigDecimal("1000")));

        assertThat(applianceLiveStateSlot.lastProcessedSequence()).isEqualTo(100L);
        verify(telemetryBillingRecorder).persist(any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), any(), anyInt());
    }

    @Test
    void treatsAMissingSequenceAsUnguardedSoPreSequenceProducersStillProcess() {
        // Sequence 0 is what a payload without the field deserializes to. Guarding on it would drop
        // every reading such a producer sends after the first, so those go through unguarded.
        when(applianceLiveStatePort.get(HOME_ID, APPLIANCE_ID))
                .thenReturn(Optional.of(applianceStateWithSequence(100L)));

        useCase.execute(reading(UUID.randomUUID(), 0L, new BigDecimal("1000")));

        verify(telemetryBillingRecorder).persist(any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), any(), anyInt());
    }

    private static org.springframework.dao.DataIntegrityViolationException processedTelemetryEventPrimaryKeyViolation() {
        org.hibernate.exception.ConstraintViolationException constraintViolation =
                new org.hibernate.exception.ConstraintViolationException("duplicate key",
                        new java.sql.SQLException("duplicate key value violates unique constraint "
                                + "\"processed_telemetry_events_pkey\"", "23505"),
                        "processed_telemetry_events_pkey");
        return new org.springframework.dao.DataIntegrityViolationException("duplicate key", constraintViolation);
    }

    @Test
    void compensatesStateAndRethrowsOnAnOptimisticLockConflictSoKafkaRetries() {
        doThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(
                "com.vegawatt.core.billing.infrastructure.BillingAccountEntity", "some-id"))
                .when(telemetryBillingRecorder).persist(any(), any(), any(), any(), any(), any(), any(), any(),
                        anyBoolean(), any(), any(), anyInt());

        UUID eventId = UUID.randomUUID();
        assertThatThrownBy(() -> useCase.execute(reading(eventId, new BigDecimal("1000"))))
                .isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);

        verify(telemetryLiveStatePort).restore(eq(HOME_ID), eq(APPLIANCE_ID), eq(eventId), anyLong(), anyLong(), any(), any());
    }

    @Test
    void secondDeliveryOfTheSameEventIsSkippedWithoutAnyStateOrBillingMutation() {
        UUID eventId = UUID.randomUUID();

        useCase.execute(reading(eventId, new BigDecimal("1000")));
        HomeLiveState homeStateAfterFirstDelivery = homeLiveStateSlot;
        ApplianceLiveState applianceStateAfterFirstDelivery = applianceLiveStateSlot;
        verify(telemetryBillingRecorder).persist(eq(eventId), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), any(), anyInt());

        when(processedTelemetryEventRepository.existsByEventId(eventId)).thenReturn(true);
        useCase.execute(reading(eventId, new BigDecimal("1000")));

        assertThat(homeLiveStateSlot).isSameAs(homeStateAfterFirstDelivery);
        assertThat(applianceLiveStateSlot).isSameAs(applianceStateAfterFirstDelivery);
        verify(telemetryBillingRecorder, org.mockito.Mockito.times(1)).persist(eq(eventId), any(), any(), any(),
                any(), any(), any(), any(), anyBoolean(), any(), any(), anyInt());
    }
}
