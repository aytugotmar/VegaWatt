package com.vegawatt.core.access.infrastructure;

import com.vegawatt.core.access.domain.HomeMembership;
import com.vegawatt.core.access.domain.HomeMembershipRepository;
import com.vegawatt.core.access.domain.HomeMembershipRole;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class HomeMembershipRepositoryAdapter implements HomeMembershipRepository {

    private final HomeMembershipJpaRepository jpaRepository;

    HomeMembershipRepositoryAdapter(HomeMembershipJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public HomeMembership save(HomeMembership membership) {
        HomeMembershipEntity saved = jpaRepository.save(toEntity(membership));
        return toDomain(saved);
    }

    @Override
    public List<HomeMembership> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(HomeMembershipRepositoryAdapter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Set<UUID> findHomeIdsByUser(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(HomeMembershipEntity::getHomeId)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean existsByUserIdAndHomeId(UUID userId, UUID homeId) {
        return jpaRepository.existsByUserIdAndHomeId(userId, homeId);
    }

    private static HomeMembershipEntity toEntity(HomeMembership membership) {
        return new HomeMembershipEntity(membership.id(), membership.homeId(), membership.userId(),
                membership.role().name(), membership.createdAt());
    }

    private static HomeMembership toDomain(HomeMembershipEntity entity) {
        return HomeMembership.reconstitute(entity.getId(), entity.getHomeId(), entity.getUserId(),
                HomeMembershipRole.valueOf(entity.getRole()), entity.getCreatedAt());
    }
}
