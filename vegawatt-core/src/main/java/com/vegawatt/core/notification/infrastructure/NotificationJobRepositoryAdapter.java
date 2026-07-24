package com.vegawatt.core.notification.infrastructure;

import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.notification.domain.NotificationJob;
import com.vegawatt.core.notification.domain.NotificationJobRepository;
import com.vegawatt.core.notification.domain.NotificationJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class NotificationJobRepositoryAdapter implements NotificationJobRepository {

    private final NotificationJobJpaRepository jpaRepository;

    NotificationJobRepositoryAdapter(NotificationJobJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public NotificationJob save(NotificationJob job) {
        jpaRepository.save(toEntity(job));
        return job;
    }

    @Override
    public boolean hasRecentJob(UUID homeId, UUID applianceId, AdvisoryTriggerType triggerType, Instant since) {
        return applianceId == null
                ? jpaRepository.existsRecentHomeLevelJob(homeId, triggerType.name(), since)
                : jpaRepository.existsRecentApplianceJob(homeId, applianceId, triggerType.name(), since);
    }

    // The lock and the lease have to share one transaction: the row locks from lockDueBatch are what
    // keep a second worker out, and they only hold until this method commits, so the lease has to be
    // written before then. I return the claimed jobs with next_attempt_at already set to the lease,
    // matching what is now in the database.
    @Override
    @Transactional
    public List<NotificationJob> claimDue(Instant now, Instant leaseUntil, int limit) {
        List<NotificationJobEntity> locked = jpaRepository.lockDueBatch(
                NotificationJobStatus.PENDING.name(), now, limit);
        if (locked.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = locked.stream().map(NotificationJobEntity::getId).toList();
        jpaRepository.extendLease(ids, leaseUntil);
        return locked.stream()
                .map(entity -> toDomain(entity, leaseUntil))
                .toList();
    }

    private static NotificationJobEntity toEntity(NotificationJob job) {
        return new NotificationJobEntity(job.id(), job.triggerEventId(), job.homeId(), job.applianceId(),
                job.triggerType().name(), job.status().name(), job.attemptCount(), job.nextAttemptAt(),
                job.lastError(), job.createdAt());
    }

    private static NotificationJob toDomain(NotificationJobEntity entity, Instant nextAttemptAt) {
        return new NotificationJob(entity.getId(), entity.getTriggerEventId(), entity.getHomeId(),
                entity.getApplianceId(), AdvisoryTriggerType.valueOf(entity.getTriggerType()),
                NotificationJobStatus.valueOf(entity.getStatus()), entity.getAttemptCount(),
                nextAttemptAt, entity.getLastError(), entity.getCreatedAt());
    }
}
