package com.vegawatt.sensors.publishing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegawatt.sensors.config.SensorsKafkaProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TelemetryEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TelemetryEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final SensorsKafkaProperties kafkaProperties;

    public TelemetryEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                                    SensorsKafkaProperties kafkaProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.kafkaProperties = kafkaProperties;
    }

    public void publish(UUID homeId, UUID applianceId, BigDecimal powerWatt, String operatingState,
                         String operatingMode, int measurementIntervalSeconds) {
        TelemetryEventPayload payload = new TelemetryEventPayload(UUID.randomUUID(), 2, Instant.now(), homeId,
                applianceId, powerWatt, operatingState, operatingMode, measurementIntervalSeconds);

        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize telemetry event for appliance {}", applianceId, e);
            return;
        }

        kafkaTemplate.send(kafkaProperties.telemetryTopic(), homeId.toString(), json)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.warn("Failed to publish telemetry event for appliance {} after producer retries",
                                applianceId, exception);
                    }
                });
    }
}
