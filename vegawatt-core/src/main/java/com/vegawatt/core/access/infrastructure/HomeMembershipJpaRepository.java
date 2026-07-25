package com.vegawatt.core.access.infrastructure;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface HomeMembershipJpaRepository extends JpaRepository<HomeMembershipEntity, UUID> {

    List<HomeMembershipEntity> findByUserId(UUID userId);

    boolean existsByUserIdAndHomeId(UUID userId, UUID homeId);
}
