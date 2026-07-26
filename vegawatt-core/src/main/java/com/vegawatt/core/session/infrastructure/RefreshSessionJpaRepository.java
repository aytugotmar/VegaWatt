package com.vegawatt.core.session.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RefreshSessionJpaRepository extends JpaRepository<RefreshSessionEntity, UUID> {

    Optional<RefreshSessionEntity> findByTokenHash(String tokenHash);

    // flushAutomatically is required alongside clearAutomatically: without it, Spring Data JPA
    // clears the persistence context WITHOUT first flushing it, silently discarding any
    // not-yet-flushed change a caller made earlier in the same transaction (e.g.
    // ChangeUserRoleUseCase's userRepository.save() right before it revokes sessions) — the entity
    // was merged into the context but its UPDATE was never written, and clear() then drops it for
    // good with no error.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RefreshSessionEntity s SET s.revokedAt = :now WHERE s.userId = :userId AND s.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") UUID userId, @Param("now") Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE RefreshSessionEntity s SET s.revokedAt = :now WHERE s.tokenHash = :tokenHash "
            + "AND s.revokedAt IS NULL AND s.expiresAt > :now")
    int revokeIfActive(@Param("tokenHash") String tokenHash, @Param("now") Instant now);
}
