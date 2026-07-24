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
}
