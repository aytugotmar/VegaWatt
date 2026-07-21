package com.vegawatt.core.auth.application;

import com.vegawatt.core.common.security.RefreshTokenHasher;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.session.domain.RefreshSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutUseCase {

    private final RefreshSessionRepository refreshSessionRepository;
    private final ClockProvider clockProvider;

    public LogoutUseCase(RefreshSessionRepository refreshSessionRepository, ClockProvider clockProvider) {
        this.refreshSessionRepository = refreshSessionRepository;
        this.clockProvider = clockProvider;
    }

    @Transactional
    public void execute(String rawRefreshToken) {
        refreshSessionRepository.findByTokenHash(RefreshTokenHasher.hash(rawRefreshToken))
                .ifPresent(session -> {
                    session.revoke(clockProvider.now());
                    refreshSessionRepository.save(session);
                });
    }
}
