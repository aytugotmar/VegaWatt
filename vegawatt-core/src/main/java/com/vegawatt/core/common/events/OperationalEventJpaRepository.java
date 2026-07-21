package com.vegawatt.core.common.events;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OperationalEventJpaRepository extends JpaRepository<OperationalEventEntity, UUID> {
}
