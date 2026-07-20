package com.vegawatt.core.common.events;

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
}
