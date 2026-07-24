package com.vegawatt.core.notification.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface NotificationJobJpaRepository extends JpaRepository<NotificationJobEntity, UUID> {

    // FOR UPDATE SKIP LOCKED is the whole point: two workers running this at once take out row
    // locks on the rows they read and skip past rows the other already locked, so each due job is
    // handed to exactly one worker instead of both. It is a PostgreSQL feature with no JPQL
    // equivalent, which is why this is a native query. The caller holds these locks until its
    // transaction commits, and within that window it bumps next_attempt_at forward, so the claim
    // survives the lock being released.
    @Query(value = """
            SELECT * FROM notification_jobs
            WHERE status = :status AND next_attempt_at <= :now
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<NotificationJobEntity> lockDueBatch(@Param("status") String status, @Param("now") Instant now,
                                             @Param("limit") int limit);

    // I clear the persistence context after the bulk update so the entities the lock query returned
    // cannot be flushed back with their pre-lease next_attempt_at and undo the claim.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NotificationJobEntity j SET j.nextAttemptAt = :leaseUntil WHERE j.id IN :ids")
    void extendLease(@Param("ids") List<UUID> ids, @Param("leaseUntil") Instant leaseUntil);

    @Query("""
            SELECT COUNT(j) > 0 FROM NotificationJobEntity j
            WHERE j.homeId = :homeId AND j.applianceId = :applianceId AND j.triggerType = :triggerType
                AND j.createdAt >= :since
            """)
    boolean existsRecentApplianceJob(@Param("homeId") UUID homeId, @Param("applianceId") UUID applianceId,
                                     @Param("triggerType") String triggerType, @Param("since") Instant since);

    @Query("""
            SELECT COUNT(j) > 0 FROM NotificationJobEntity j
            WHERE j.homeId = :homeId AND j.applianceId IS NULL AND j.triggerType = :triggerType
                AND j.createdAt >= :since
            """)
    boolean existsRecentHomeLevelJob(@Param("homeId") UUID homeId, @Param("triggerType") String triggerType,
                                     @Param("since") Instant since);
}
