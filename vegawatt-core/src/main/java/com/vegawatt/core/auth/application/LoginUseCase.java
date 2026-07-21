package com.vegawatt.core.auth.application;

import com.vegawatt.core.auth.domain.InvalidCredentialsException;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshSessionRepository refreshSessionRepository;
    private final JwtProperties jwtProperties;
    private final ClockProvider clockProvider;

    public LoginUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder,
                         JwtTokenService jwtTokenService, RefreshSessionRepository refreshSessionRepository,
                         JwtProperties jwtProperties, ClockProvider clockProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshSessionRepository = refreshSessionRepository;
        this.jwtProperties = jwtProperties;
        this.clockProvider = clockProvider;
    }

    @Transactional
    public AuthSession execute(String email, String rawPassword) {
        User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(rawPassword, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueSession(user);
    }

    public AuthSession issueSession(User user) {
        Instant now = clockProvider.now();
        String accessToken = jwtTokenService.generateAccessToken(user.id(), user.role(), now);
        String rawRefreshToken = OpaqueTokenGenerator.generate();
        Instant refreshExpiresAt = now.plus(Duration.ofDays(jwtProperties.refreshTokenTtlDays()));
        refreshSessionRepository.save(RefreshSession.issue(user.id(), RefreshTokenHasher.hash(rawRefreshToken),
                refreshExpiresAt, now));
        return new AuthSession(user, accessToken, rawRefreshToken, refreshExpiresAt);
    }
}
