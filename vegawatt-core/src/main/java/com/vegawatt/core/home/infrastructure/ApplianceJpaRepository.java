package com.vegawatt.core.home.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ApplianceJpaRepository extends JpaRepository<ApplianceEntity, UUID> {

    List<ApplianceEntity> findByHomeId(UUID homeId);
}
