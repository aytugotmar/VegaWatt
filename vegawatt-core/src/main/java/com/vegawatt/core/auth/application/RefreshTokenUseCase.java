package com.vegawatt.core.auth.application;

import com.vegawatt.core.auth.domain.InvalidRefreshTokenException;
import com.vegawatt.core.common.config.JwtProperties;
import com.vegawatt.core.common.security.JwtTokenService;
import com.vegawatt.core.common.security.OpaqueTokenGenerator;
import com.vegawatt.core.common.security.RefreshTokenHasher;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.session.domain.RefreshSession;
import com.vegawatt.core.session.domain.RefreshSessionRepository;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenUseCase {

    private final RefreshSessionRepository refreshSessionRepository;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final ClockProvider clockProvider;

    public RefreshTokenUseCase(RefreshSessionRepository refreshSessionRepository, UserRepository userRepository,
                                JwtTokenService jwtTokenService, JwtProperties jwtProperties,
                                ClockProvider clockProvider) {
        this.refreshSessionRepository = refreshSessionRepository;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.clockProvider = clockProvider;
    }

    @Transactional
    public AuthSession execute(String rawRefreshToken) {
        RefreshSession session = refreshSessionRepository.findByTokenHash(RefreshTokenHasher.hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        Instant now = clockProvider.now();
        if (!session.isActive(now)) {
            throw new InvalidRefreshTokenException();
        }
        // The isActive() check above is just a fast rejection for an obviously-dead session — the
        // real guard is this atomic conditional UPDATE, which only one of two concurrent requests
        // presenting the same token can win (see RefreshSessionRepository#revokeIfActive).
        if (!refreshSessionRepository.revokeIfActive(session.tokenHash(), now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(session.userId()).orElseThrow(InvalidRefreshTokenException::new);

        String accessToken = jwtTokenService.generateAccessToken(user.id(), user.role(), now);
        String newRawRefreshToken = OpaqueTokenGenerator.generate();
        Instant refreshExpiresAt = now.plus(Duration.ofDays(jwtProperties.refreshTokenTtlDays()));
        refreshSessionRepository.save(RefreshSession.issue(user.id(), RefreshTokenHasher.hash(newRawRefreshToken),
                refreshExpiresAt, now));

        return new AuthSession(user, accessToken, newRawRefreshToken, refreshExpiresAt);
    }
}
