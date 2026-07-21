package com.vegawatt.core.session.domain;

import java.util.Optional;

public interface RefreshSessionRepository {

    RefreshSession save(RefreshSession session);

    Optional<RefreshSession> findByTokenHash(String tokenHash);
}
