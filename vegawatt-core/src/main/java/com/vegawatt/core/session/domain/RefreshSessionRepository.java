package com.vegawatt.core.session.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository {

    RefreshSession save(RefreshSession session);

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    /** Revokes every currently-active session for this user — called after a password or role
     * change so a refresh token issued before the change (e.g. one already stolen) stops working
     * immediately instead of remaining valid until it naturally expires. */
    void revokeAllByUserId(UUID userId, Instant now);

    /** Atomically revokes the session for this token hash only if it is still active (not already
     * revoked, not expired), returning true iff this call was the one that revoked it. Refresh
     * token rotation must use this — a plain findByTokenHash() + isActive() check + save() is a
     * read-then-write race: two concurrent requests presenting the same refresh token could both
     * pass the check before either commits its revoke, letting both mint a new session from a
     * single presented token. */
    boolean revokeIfActive(String tokenHash, Instant now);
}
