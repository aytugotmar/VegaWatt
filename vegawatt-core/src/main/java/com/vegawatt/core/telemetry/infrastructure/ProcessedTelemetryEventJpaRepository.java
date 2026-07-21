package com.vegawatt.core.telemetry.infrastructure;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProcessedTelemetryEventJpaRepository extends JpaRepository<ProcessedTelemetryEventEntity, UUID> {
}
