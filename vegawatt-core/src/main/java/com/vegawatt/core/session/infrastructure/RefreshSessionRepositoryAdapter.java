package com.vegawatt.core.session.infrastructure;

import com.vegawatt.core.session.domain.RefreshSession;
import com.vegawatt.core.session.domain.RefreshSessionRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

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

    private static RefreshSessionEntity toEntity(RefreshSession session) {
        return new RefreshSessionEntity(session.id(), session.userId(), session.tokenHash(), session.expiresAt(),
                session.revokedAt(), session.createdAt());
    }

    private static RefreshSession toDomain(RefreshSessionEntity entity) {
        return RefreshSession.reconstitute(entity.getId(), entity.getUserId(), entity.getTokenHash(),
                entity.getExpiresAt(), entity.getCreatedAt(), entity.getRevokedAt());
    }
}
