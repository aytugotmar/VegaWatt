package com.vegawatt.core.anomaly.application;

import com.vegawatt.core.anomaly.domain.EvaluateStandbyConsumptionPolicy;
import com.vegawatt.core.anomaly.domain.StandbyAnomalyEvaluationResult;
import com.vegawatt.core.common.config.StandbyAnomalyProperties;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Application-layer entry point for {@link EvaluateStandbyConsumptionPolicy}, wrapping it with the
 * externally configured threshold/hysteresis parameters. Mirrors
 * {@link com.vegawatt.core.anomaly.application.EvaluateApplianceAnomalyUseCase}'s shape.
 */
@Service
public class EvaluateStandbyConsumptionUseCase {

    private final StandbyAnomalyProperties standbyAnomalyProperties;

    public EvaluateStandbyConsumptionUseCase(StandbyAnomalyProperties standbyAnomalyProperties) {
        this.standbyAnomalyProperties = standbyAnomalyProperties;
    }

    public StandbyAnomalyEvaluationResult evaluate(int previousBreachCount, int previousRecoveryCount,
                                                     boolean previouslyActive, BigDecimal currentPowerWatt,
                                                     BigDecimal standbyMaxWatt) {
        return EvaluateStandbyConsumptionPolicy.evaluate(previousBreachCount, previousRecoveryCount, previouslyActive,
                currentPowerWatt, standbyMaxWatt, standbyAnomalyProperties.thresholdMultiplier(),
                standbyAnomalyProperties.minimumExcessWatt(), standbyAnomalyProperties.breachThreshold(),
                standbyAnomalyProperties.recoveryThreshold());
    }
}
