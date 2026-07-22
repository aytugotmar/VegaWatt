package com.vegawatt.core.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.Money;
import com.vegawatt.core.common.TariffState;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.notification.domain.AdvisoryContext;
import com.vegawatt.core.notification.domain.AdvisoryResult;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.notification.domain.AiRecommendation;
import com.vegawatt.core.notification.domain.AiRecommendationRepository;
import com.vegawatt.core.notification.domain.EnergyAdvisoryPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenerateEnergyAdvisoryUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID HOME_ID = UUID.randomUUID();
    private static final UUID TRIGGER_EVENT_ID = UUID.randomUUID();

    @Mock
    private EnergyAdvisoryPort energyAdvisoryPort;
    @Mock
    private AiRecommendationRepository aiRecommendationRepository;
    @Mock
    private ClockProvider clockProvider;

    private static AdvisoryContext context() {
        return new AdvisoryContext(HOME_ID, "Test Ev", AdvisoryTriggerType.QUOTA_80, new BigDecimal("85"),
                new BigDecimal("40"), Money.of(new BigDecimal("120.00")), TariffState.BASE, List.of(),
                TRIGGER_EVENT_ID);
    }

    @Test
    void generatesAndStoresAnAdvisoryWhenNoneExistsForTheEvent() {
        when(aiRecommendationRepository.findByTriggerEventId(TRIGGER_EVENT_ID)).thenReturn(Optional.empty());
        when(clockProvider.now()).thenReturn(NOW);
        when(energyAdvisoryPort.generateAdvisory(any())).thenReturn(new AdvisoryResult("Tasarruf öneriniz", false));
        when(aiRecommendationRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        GenerateEnergyAdvisoryUseCase useCase = new GenerateEnergyAdvisoryUseCase(energyAdvisoryPort,
                aiRecommendationRepository, clockProvider);
        AiRecommendation result = useCase.execute(context());

        assertThat(result.content()).isEqualTo("Tasarruf öneriniz");
        assertThat(result.triggerEventId()).isEqualTo(TRIGGER_EVENT_ID);
        verify(energyAdvisoryPort).generateAdvisory(any());
    }

    @Test
    void reusesTheExistingAdvisoryOnRetryWithoutCallingTheModelAgain() {
        AiRecommendation alreadyGenerated = AiRecommendation.create(HOME_ID, AdvisoryTriggerType.QUOTA_80,
                "İlk üretilen öneri", false, NOW, TRIGGER_EVENT_ID);
        when(aiRecommendationRepository.findByTriggerEventId(TRIGGER_EVENT_ID))
                .thenReturn(Optional.of(alreadyGenerated));

        GenerateEnergyAdvisoryUseCase useCase = new GenerateEnergyAdvisoryUseCase(energyAdvisoryPort,
                aiRecommendationRepository, clockProvider);
        AiRecommendation result = useCase.execute(context());

        // This runs on the retry path: a job that generated an advisory and then failed to email
        // it comes back here on every attempt. Without the lookup that is up to five model calls
        // and five rows for one event, and a retry during an outage would quietly replace real
        // advice with fallback text.
        assertThat(result).isEqualTo(alreadyGenerated);
        assertThat(result.content()).isEqualTo("İlk üretilen öneri");
        verify(energyAdvisoryPort, never()).generateAdvisory(any());
        verify(aiRecommendationRepository, never()).save(any());
    }
}
