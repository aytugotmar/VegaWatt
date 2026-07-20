package com.vegawatt.core.billing.infrastructure;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface BillingAccountJpaRepository extends JpaRepository<BillingAccountEntity, UUID> {

    Optional<BillingAccountEntity> findByHomeId(UUID homeId);
}
