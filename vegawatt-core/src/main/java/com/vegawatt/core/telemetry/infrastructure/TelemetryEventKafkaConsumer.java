package com.vegawatt.core.telemetry.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegawatt.core.telemetry.application.ProcessTelemetryUseCase;
import com.vegawatt.core.telemetry.domain.TelemetryReading;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class TelemetryEventKafkaConsumer {

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
        return new TelemetryReading(payload.eventId(), payload.homeId(), payload.applianceId(), payload.powerWatt(),
                payload.operatingState(), payload.operatingMode(), payload.measurementIntervalSeconds(),
                payload.occurredAt());
    }
}
