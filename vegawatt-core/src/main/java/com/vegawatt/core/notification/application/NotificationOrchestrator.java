package com.vegawatt.core.notification.application;

import com.vegawatt.core.home.domain.Appliance;
import com.vegawatt.core.home.domain.ApplianceLiveState;
import com.vegawatt.core.home.domain.ApplianceLiveStatePort;
import com.vegawatt.core.home.domain.Home;
import com.vegawatt.core.home.domain.HomeLiveState;
import com.vegawatt.core.home.domain.HomeLiveStatePort;
import com.vegawatt.core.home.domain.HomeRepository;
import com.vegawatt.core.notification.domain.AdvisoryContext;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.notification.domain.AiRecommendation;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Entry point for asynchronous advisory generation. Runs on the bounded executor configured in
 * {@code AsyncConfig} so a slow Gemini call or SMTP send never blocks the Kafka consumer thread
 * that triggers it.
 */
@Service
public class NotificationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NotificationOrchestrator.class);

    private final HomeRepository homeRepository;
    private final HomeLiveStatePort homeLiveStatePort;
    private final ApplianceLiveStatePort applianceLiveStatePort;
    private final GenerateEnergyAdvisoryUseCase generateEnergyAdvisoryUseCase;
    private final DispatchAdvisoryEmailUseCase dispatchAdvisoryEmailUseCase;

    public NotificationOrchestrator(HomeRepository homeRepository, HomeLiveStatePort homeLiveStatePort,
                                     ApplianceLiveStatePort applianceLiveStatePort,
                                     GenerateEnergyAdvisoryUseCase generateEnergyAdvisoryUseCase,
                                     DispatchAdvisoryEmailUseCase dispatchAdvisoryEmailUseCase) {
        this.homeRepository = homeRepository;
        this.homeLiveStatePort = homeLiveStatePort;
        this.applianceLiveStatePort = applianceLiveStatePort;
        this.generateEnergyAdvisoryUseCase = generateEnergyAdvisoryUseCase;
        this.dispatchAdvisoryEmailUseCase = dispatchAdvisoryEmailUseCase;
    }

    @Async
    public void triggerAdvisory(UUID homeId, AdvisoryTriggerType triggerType) {
        try {
            Home home = homeRepository.findById(homeId).orElse(null);
            HomeLiveState liveState = homeLiveStatePort.get(homeId).orElse(null);
            if (home == null || liveState == null) {
                log.warn("Skipping advisory for home {}: missing home or live state", homeId);
                return;
            }

            List<String> anomalousApplianceNames = applianceLiveStatePort.getByHomeId(homeId).stream()
                    .filter(ApplianceLiveState::anomalous)
                    .map(state -> applianceName(home, state.applianceId()))
                    .toList();

            AdvisoryContext context = new AdvisoryContext(homeId, home.name(), triggerType,
                    liveState.energyQuotaPercentage(), liveState.budgetQuotaPercentage(), liveState.currentCost(),
                    liveState.tariffState(), anomalousApplianceNames);

            AiRecommendation recommendation = generateEnergyAdvisoryUseCase.execute(context);
            dispatchAdvisoryEmailUseCase.execute(recommendation, home.contactEmail());
        } catch (RuntimeException e) {
            log.error("Failed to generate or send advisory for home {}", homeId, e);
        }
    }

    private static String applianceName(Home home, UUID applianceId) {
        return home.appliances().stream()
                .filter(appliance -> appliance.id().equals(applianceId))
                .map(Appliance::name)
                .findFirst()
                .orElse(applianceId.toString());
    }
}
