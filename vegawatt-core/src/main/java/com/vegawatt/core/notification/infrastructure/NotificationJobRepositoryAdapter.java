package com.vegawatt.core.notification.infrastructure;

import com.vegawatt.core.notification.domain.AdvisoryTriggerType;
import com.vegawatt.core.notification.domain.NotificationJob;
import com.vegawatt.core.notification.domain.NotificationJobRepository;
import com.vegawatt.core.notification.domain.NotificationJobStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

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
    public List<NotificationJob> findDue(Instant now, int limit) {
        return jpaRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        NotificationJobStatus.PENDING.name(), now, PageRequest.of(0, limit))
                .stream()
                .map(NotificationJobRepositoryAdapter::toDomain)
                .toList();
    }

    private static NotificationJobEntity toEntity(NotificationJob job) {
        return new NotificationJobEntity(job.id(), job.triggerEventId(), job.homeId(), job.triggerType().name(),
                job.status().name(), job.attemptCount(), job.nextAttemptAt(), job.lastError(), job.createdAt());
    }

    private static NotificationJob toDomain(NotificationJobEntity entity) {
        return new NotificationJob(entity.getId(), entity.getTriggerEventId(), entity.getHomeId(),
                AdvisoryTriggerType.valueOf(entity.getTriggerType()),
                NotificationJobStatus.valueOf(entity.getStatus()), entity.getAttemptCount(),
                entity.getNextAttemptAt(), entity.getLastError(), entity.getCreatedAt());
    }
}
