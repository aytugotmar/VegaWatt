package com.vegawatt.core.session.domain;

import java.time.Instant;
import java.util.UUID;

public final class RefreshSession {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant revokedAt;

    private RefreshSession(UUID id, UUID userId, String tokenHash, Instant expiresAt, Instant createdAt,
                            Instant revokedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
    }

    public static RefreshSession issue(UUID userId, String tokenHash, Instant expiresAt, Instant createdAt) {
        return new RefreshSession(UUID.randomUUID(), userId, tokenHash, expiresAt, createdAt, null);
    }

    public static RefreshSession reconstitute(UUID id, UUID userId, String tokenHash, Instant expiresAt,
                                               Instant createdAt, Instant revokedAt) {
        return new RefreshSession(id, userId, tokenHash, expiresAt, createdAt, revokedAt);
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(Instant now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant revokedAt() {
        return revokedAt;
    }
}
