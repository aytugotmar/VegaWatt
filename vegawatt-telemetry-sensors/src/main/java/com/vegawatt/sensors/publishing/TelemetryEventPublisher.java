package com.vegawatt.sensors.publishing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegawatt.sensors.config.SensorsKafkaProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
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

    // I seed each appliance's counter from the process start epoch (millis) instead of zero so the
    // sequence keeps climbing across a sensor restart. The core out-of-order guard keeps the last
    // sequence it saw per appliance and drops anything lower, so a counter that reset to zero on
    // restart would make every reading the restarted sensor sends look stale and get dropped. The
    // wall clock only moves forward, and at our per-second emit rate the new base always outruns the
    // last number the previous run reached, so per-appliance monotonicity survives a restart. Each
    // appliance publishes on its own scheduled task, so the map and counters have to be concurrent.
    private final long sequenceBase = Instant.now().toEpochMilli();
    private final ConcurrentHashMap<UUID, AtomicLong> sequenceByAppliance = new ConcurrentHashMap<>();

    public TelemetryEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                                    SensorsKafkaProperties kafkaProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.kafkaProperties = kafkaProperties;
    }

    public void publish(UUID homeId, UUID applianceId, BigDecimal powerWatt, String operatingState,
                         String operatingMode, int measurementIntervalSeconds) {
        long sequenceNumber = sequenceByAppliance
                .computeIfAbsent(applianceId, id -> new AtomicLong(sequenceBase))
                .incrementAndGet();
        TelemetryEventPayload payload = new TelemetryEventPayload(UUID.randomUUID(), 2, Instant.now(), homeId,
                applianceId, sequenceNumber, powerWatt, operatingState, operatingMode, measurementIntervalSeconds);

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
