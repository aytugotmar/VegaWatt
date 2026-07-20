package com.vegawatt.core.telemetry.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vegawatt.core.common.config.VegaWattKafkaProperties;
import com.vegawatt.core.home.domain.ApplianceNotFoundException;
import com.vegawatt.core.home.domain.HomeNotFoundException;
import com.vegawatt.core.telemetry.application.ProcessTelemetryUseCase;
import com.vegawatt.core.telemetry.domain.TelemetryReading;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class TelemetryEventKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(TelemetryEventKafkaConsumer.class);
    private static final String DEAD_LETTER_SUFFIX = ".DLT";

    private final ProcessTelemetryUseCase processTelemetryUseCase;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final VegaWattKafkaProperties kafkaProperties;

    TelemetryEventKafkaConsumer(ProcessTelemetryUseCase processTelemetryUseCase, ObjectMapper objectMapper,
                                 KafkaTemplate<String, String> kafkaTemplate,
                                 VegaWattKafkaProperties kafkaProperties) {
        this.processTelemetryUseCase = processTelemetryUseCase;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
    }

    @KafkaListener(topics = "${vegawatt.kafka.telemetry-topic}", groupId = "vegawatt-core-telemetry")
    void onMessage(ConsumerRecord<String, String> record) {
        TelemetryReading reading;
        try {
            reading = parse(record.value());
        } catch (RuntimeException e) {
            log.warn("Discarding malformed telemetry event from partition {} offset {}, routing to dead letter topic",
                    record.partition(), record.offset(), e);
            publishToDeadLetterTopic(record.key(), record.value());
            return;
        }

        try {
            processTelemetryUseCase.execute(reading);
        } catch (HomeNotFoundException | ApplianceNotFoundException e) {
            log.warn("Skipping telemetry event {}: {}", reading.eventId(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("Failed to process telemetry event {}, routing to dead letter topic", reading.eventId(), e);
            publishToDeadLetterTopic(record.key(), record.value());
        }
    }

    private TelemetryReading parse(String rawValue) {
        TelemetryEventPayload payload;
        try {
            payload = objectMapper.readValue(rawValue, TelemetryEventPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Malformed telemetry event JSON", e);
        }
        return new TelemetryReading(payload.eventId(), payload.homeId(), payload.applianceId(), payload.powerWatt(),
                payload.measurementIntervalSeconds(), payload.occurredAt());
    }

    private void publishToDeadLetterTopic(String key, String rawValue) {
        String deadLetterTopic = kafkaProperties.telemetryTopic() + DEAD_LETTER_SUFFIX;
        kafkaTemplate.send(deadLetterTopic, key, rawValue);
    }
}
