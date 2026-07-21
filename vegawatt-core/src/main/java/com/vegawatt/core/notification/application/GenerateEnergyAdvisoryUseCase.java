package com.vegawatt.core.notification.application;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.notification.domain.AdvisoryContext;
import com.vegawatt.core.notification.domain.AdvisoryResult;
import com.vegawatt.core.notification.domain.AiRecommendation;
import com.vegawatt.core.notification.domain.AiRecommendationRepository;
import com.vegawatt.core.notification.domain.EnergyAdvisoryPort;
import org.springframework.stereotype.Service;

@Service
public class GenerateEnergyAdvisoryUseCase {

    private final EnergyAdvisoryPort energyAdvisoryPort;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final ClockProvider clockProvider;

    public GenerateEnergyAdvisoryUseCase(EnergyAdvisoryPort energyAdvisoryPort,
                                          AiRecommendationRepository aiRecommendationRepository,
                                          ClockProvider clockProvider) {
        this.energyAdvisoryPort = energyAdvisoryPort;
        this.aiRecommendationRepository = aiRecommendationRepository;
        this.clockProvider = clockProvider;
    }

    public AiRecommendation execute(AdvisoryContext context) {
        AdvisoryResult result = energyAdvisoryPort.generateAdvisory(context);
        AiRecommendation recommendation = AiRecommendation.create(context.homeId(), context.triggerType(),
                result.content(), result.fallbackUsed(), clockProvider.now(), context.triggerEventId());
        return aiRecommendationRepository.save(recommendation);
    }
}
