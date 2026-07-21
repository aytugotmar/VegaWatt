package com.vegawatt.core.notification.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationJobJpaRepository extends JpaRepository<NotificationJobEntity, UUID> {

    List<NotificationJobEntity> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(String status,
                                                                                              Instant now,
                                                                                              Pageable pageable);
}
