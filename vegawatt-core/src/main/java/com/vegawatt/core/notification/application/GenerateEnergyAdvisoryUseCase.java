package com.vegawatt.core.notification.application;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.notification.domain.AdvisoryContext;
import com.vegawatt.core.notification.domain.AdvisoryResult;
import com.vegawatt.core.notification.domain.AiRecommendation;
import com.vegawatt.core.notification.domain.AiRecommendationRepository;
import com.vegawatt.core.notification.domain.EnergyAdvisoryPort;
import java.util.Optional;
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

    /**
     * Returns the advisory for this operational event, generating it only if one does not exist
     * yet.
     *
     * <p>The lookup exists because this runs on a retry path. Without it, a job that generated an
     * advisory and then failed to email it would call the model again on every attempt: up to
     * five model calls and five rows for one event, each row a slightly different piece of advice
     * about the same thing. Reusing the first one also means a retry cannot turn a real advisory
     * into a fallback one just because the model happened to be down the second time.
     */
    public AiRecommendation execute(AdvisoryContext context) {
        Optional<AiRecommendation> existing =
                aiRecommendationRepository.findByTriggerEventId(context.triggerEventId());
        if (existing.isPresent()) {
            return existing.get();
        }

        AdvisoryResult result = energyAdvisoryPort.generateAdvisory(context);
        AiRecommendation recommendation = AiRecommendation.create(context.homeId(), context.triggerType(),
                result.content(), result.fallbackUsed(), clockProvider.now(), context.triggerEventId());
        return aiRecommendationRepository.save(recommendation);
    }
}
