package com.vegawatt.core.telemetry.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegawatt.core.telemetry.application.ProcessTelemetryUseCase;
import com.vegawatt.core.telemetry.domain.TelemetryReading;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class TelemetryEventKafkaConsumer {

    /** v1: no sequenceNumber/operatingState/operatingMode. v2: adds them. */
    private static final Set<Integer> SUPPORTED_EVENT_VERSIONS = Set.of(1, 2);

    private final ProcessTelemetryUseCase processTelemetryUseCase;
    private final ObjectMapper objectMapper;

    TelemetryEventKafkaConsumer(ProcessTelemetryUseCase processTelemetryUseCase, ObjectMapper objectMapper) {
        this.processTelemetryUseCase = processTelemetryUseCase;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${vegawatt.kafka.telemetry-topic}", groupId = "vegawatt-core-telemetry")
    void onMessage(ConsumerRecord<String, String> record) {
        TelemetryReading reading = parse(record.value());
        processTelemetryUseCase.execute(reading);
    }

    private TelemetryReading parse(String rawValue) {
        TelemetryEventPayload payload;
        try {
            payload = objectMapper.readValue(rawValue, TelemetryEventPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed telemetry event JSON", e);
        }

        if (!SUPPORTED_EVENT_VERSIONS.contains(payload.eventVersion())) {
            throw new UnsupportedTelemetryEventVersionException(
                    "Telemetry event %s has unsupported eventVersion %d".formatted(payload.eventId(),
                            payload.eventVersion()));
        }

        return new TelemetryReading(payload.eventId(), payload.homeId(), payload.applianceId(),
                payload.sequenceNumber(), payload.powerWatt(), payload.operatingState(), payload.operatingMode(),
                payload.measurementIntervalSeconds(), payload.occurredAt());
    }
}
