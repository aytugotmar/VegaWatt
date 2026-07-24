package com.vegawatt.core.session.infrastructure;

import com.vegawatt.core.session.domain.RefreshSession;
import com.vegawatt.core.session.domain.RefreshSessionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class RefreshSessionRepositoryAdapter implements RefreshSessionRepository {

    private final RefreshSessionJpaRepository jpaRepository;

    RefreshSessionRepositoryAdapter(RefreshSessionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public RefreshSession save(RefreshSession session) {
        RefreshSessionEntity saved = jpaRepository.save(toEntity(session));
        return toDomain(saved);
    }

    @Override
    public Optional<RefreshSession> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(RefreshSessionRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(UUID userId, Instant now) {
        jpaRepository.revokeAllByUserId(userId, now);
    }

    private static RefreshSessionEntity toEntity(RefreshSession session) {
        return new RefreshSessionEntity(session.id(), session.userId(), session.tokenHash(), session.expiresAt(),
                session.revokedAt(), session.createdAt());
    }

    private static RefreshSession toDomain(RefreshSessionEntity entity) {
        return RefreshSession.reconstitute(entity.getId(), entity.getUserId(), entity.getTokenHash(),
                entity.getExpiresAt(), entity.getCreatedAt(), entity.getRevokedAt());
    }
}
