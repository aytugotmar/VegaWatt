package com.vegawatt.core.access.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class HomeAccessService {

    private final HomeMembershipRepository homeMembershipRepository;

    public HomeAccessService(HomeMembershipRepository homeMembershipRepository) {
        this.homeMembershipRepository = homeMembershipRepository;
    }

    public Set<UUID> accessibleHomeIds(UUID userId) {
        return homeMembershipRepository.findHomeIdsByUser(userId);
    }

    public boolean canAccess(UUID userId, UUID homeId) {
        return homeMembershipRepository.existsByUserIdAndHomeId(userId, homeId);
    }

    public void grantOwnership(UUID homeId, UUID userId, Instant now) {
        homeMembershipRepository.save(HomeMembership.grantOwnership(homeId, userId, now));
    }
}
