package com.vegawatt.core.anomaly.application;

import com.vegawatt.core.common.ApplianceHealthStatus;
import com.vegawatt.core.common.events.OperationalEvent;
import com.vegawatt.core.common.events.OperationalEventRepository;
import com.vegawatt.core.common.events.OperationalEventType;
import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.notification.domain.NotificationJob;
import com.vegawatt.core.notification.domain.NotificationJobRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryHealthTransitionRecorder {

    private final OperationalEventRepository operationalEventRepository;
    private final NotificationJobRepository notificationJobRepository;

    public TelemetryHealthTransitionRecorder(OperationalEventRepository operationalEventRepository,
                                             NotificationJobRepository notificationJobRepository) {
        this.operationalEventRepository = operationalEventRepository;
        this.notificationJobRepository = notificationJobRepository;
    }

    @Transactional
    public void record(UUID homeId, UUID applianceId, ApplianceHealthStatus next, Instant now) {
        boolean offline = next == ApplianceHealthStatus.OFFLINE;
        OperationalEventType eventType = offline ? OperationalEventType.APPLIANCE_TELEMETRY_OFFLINE
                : OperationalEventType.APPLIANCE_TELEMETRY_STALE;
        AdvisoryTriggerType triggerType = offline ? AdvisoryTriggerType.TELEMETRY_OFFLINE
                : AdvisoryTriggerType.TELEMETRY_STALE;
        String details = "telemetry health transitioned to %s, detectedAt=%s".formatted(next, now);

        OperationalEvent saved = operationalEventRepository.save(
                OperationalEvent.create(homeId, applianceId, eventType, now, details));
        notificationJobRepository.save(NotificationJob.create(saved.id(), homeId, triggerType, now));
    }
}
