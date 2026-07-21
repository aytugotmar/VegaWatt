package com.vegawatt.core.access.domain;

import java.time.Instant;
import java.util.UUID;

public final class HomeMembership {

    private final UUID id;
    private final UUID homeId;
    private final UUID userId;
    private final HomeMembershipRole role;
    private final Instant createdAt;

    private HomeMembership(UUID id, UUID homeId, UUID userId, HomeMembershipRole role, Instant createdAt) {
        this.id = id;
        this.homeId = homeId;
        this.userId = userId;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static HomeMembership grantOwnership(UUID homeId, UUID userId, Instant createdAt) {
        return new HomeMembership(UUID.randomUUID(), homeId, userId, HomeMembershipRole.OWNER, createdAt);
    }

    public static HomeMembership reconstitute(UUID id, UUID homeId, UUID userId, HomeMembershipRole role,
                                               Instant createdAt) {
        return new HomeMembership(id, homeId, userId, role, createdAt);
    }

    public UUID id() {
        return id;
    }

    public UUID homeId() {
        return homeId;
    }

    public UUID userId() {
        return userId;
    }

    public HomeMembershipRole role() {
        return role;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
