package com.vegawatt.core.anomaly.application;

import com.vegawatt.core.anomaly.domain.AnomalyEvaluationResult;
import com.vegawatt.core.anomaly.domain.EvaluateApplianceAnomalyPolicy;
import com.vegawatt.core.common.config.AnomalyProperties;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Application-layer entry point for the Tariff and Anomaly Rules module's device-check
 * responsibility. Wraps the pure {@link EvaluateApplianceAnomalyPolicy} with the
 * externally configured consecutive-breach threshold.
 */
@Service
public class EvaluateApplianceAnomalyUseCase {

    private final AnomalyProperties anomalyProperties;

    public EvaluateApplianceAnomalyUseCase(AnomalyProperties anomalyProperties) {
        this.anomalyProperties = anomalyProperties;
    }

    public AnomalyEvaluationResult evaluate(int previousConsecutiveBreachCount, int previousConsecutiveNormalCount,
                                             boolean previouslyAnomalous, BigDecimal currentPowerWatt,
                                             BigDecimal safePowerLimitWatt) {
        return EvaluateApplianceAnomalyPolicy.evaluate(previousConsecutiveBreachCount, previousConsecutiveNormalCount,
                previouslyAnomalous, currentPowerWatt, safePowerLimitWatt, anomalyProperties.breachThreshold(),
                anomalyProperties.recoveryThreshold(), anomalyProperties.recoveryRatio());
    }

    public int breachThreshold() {
        return anomalyProperties.breachThreshold();
    }
}
