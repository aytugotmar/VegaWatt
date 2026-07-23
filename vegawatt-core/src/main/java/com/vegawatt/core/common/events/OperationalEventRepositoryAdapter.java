package com.vegawatt.core.common.events;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OperationalEventRepositoryAdapter implements OperationalEventRepository {

    private final OperationalEventJpaRepository jpaRepository;

    OperationalEventRepositoryAdapter(OperationalEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public OperationalEvent save(OperationalEvent event) {
        OperationalEventEntity entity = new OperationalEventEntity(event.id(), event.homeId(), event.applianceId(),
                event.eventType().name(), event.eventTime(), event.details());
        jpaRepository.save(entity);
        return event;
    }

    @Override
    public List<OperationalEvent> findByHomeId(UUID homeId) {
        return jpaRepository.findByHomeIdOrderByEventTimeDesc(homeId).stream()
                .map(OperationalEventRepositoryAdapter::toDomain)
                .toList();
    }

    private static OperationalEvent toDomain(OperationalEventEntity entity) {
        return new OperationalEvent(entity.getId(), entity.getHomeId(), entity.getApplianceId(),
                OperationalEventType.valueOf(entity.getEventType()), entity.getEventTime(), entity.getDetails());
    }
}
