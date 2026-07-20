package com.vegawatt.core.telemetry.infrastructure;

import com.vegawatt.core.telemetry.domain.ProcessedTelemetryEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ProcessedTelemetryEventRepositoryAdapter implements ProcessedTelemetryEventRepository {

    private final ProcessedTelemetryEventJpaRepository jpaRepository;

    ProcessedTelemetryEventRepositoryAdapter(ProcessedTelemetryEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsByEventId(UUID eventId) {
        return jpaRepository.existsById(eventId);
    }

    @Override
    public void markProcessed(UUID eventId, UUID homeId, UUID applianceId, Instant processedAt) {
        jpaRepository.save(new ProcessedTelemetryEventEntity(eventId, homeId, applianceId, processedAt));
    }
}
