package com.vegawatt.core.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.auth.domain.InvalidRefreshTokenException;
import com.vegawatt.core.common.config.JwtProperties;
import com.vegawatt.core.common.security.JwtTokenService;
import com.vegawatt.core.common.security.RefreshTokenHasher;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.session.domain.RefreshSession;
import com.vegawatt.core.session.domain.RefreshSessionRepository;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserRepository;
import com.vegawatt.core.user.domain.UserRole;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    private final JwtProperties jwtProperties = new JwtProperties("test-secret", 15, 30);
    private final ClockProvider clockProvider = () -> Instant.parse("2026-07-20T10:00:00Z");

    @Test
    void rotatesRefreshTokenAndIssuesNewAccessToken() {
        User user = User.reconstitute(UUID.randomUUID(), "ayse@example.com", "encoded-hash", UserRole.USER,
                Instant.parse("2026-01-01T00:00:00Z"));
        RefreshSession existingSession = RefreshSession.issue(user.id(), RefreshTokenHasher.hash("raw-token"),
                Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-07-01T00:00:00Z"));

        when(refreshSessionRepository.findByTokenHash(RefreshTokenHasher.hash("raw-token")))
                .thenReturn(Optional.of(existingSession));
        when(refreshSessionRepository.revokeIfActive(existingSession.tokenHash(),
                Instant.parse("2026-07-20T10:00:00Z"))).thenReturn(true);
        when(userRepository.findById(user.id())).thenReturn(Optional.of(user));
        when(jwtTokenService.generateAccessToken(user.id(), UserRole.USER, Instant.parse("2026-07-20T10:00:00Z")))
                .thenReturn("new-access-token");
        when(refreshSessionRepository.save(any(RefreshSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenUseCase useCase = new RefreshTokenUseCase(refreshSessionRepository, userRepository,
                jwtTokenService, jwtProperties, clockProvider);
        AuthSession result = useCase.execute("raw-token");

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.rawRefreshToken()).isNotEqualTo("raw-token");
        verify(refreshSessionRepository).revokeIfActive(existingSession.tokenHash(),
                Instant.parse("2026-07-20T10:00:00Z"));
    }

    @Test
    void rejectsWhenAnotherConcurrentRequestAlreadyWonTheRevokeRace() {
        User user = User.reconstitute(UUID.randomUUID(), "ayse@example.com", "encoded-hash", UserRole.USER,
                Instant.parse("2026-01-01T00:00:00Z"));
        RefreshSession existingSession = RefreshSession.issue(user.id(), RefreshTokenHasher.hash("raw-token"),
                Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-07-01T00:00:00Z"));

        when(refreshSessionRepository.findByTokenHash(RefreshTokenHasher.hash("raw-token")))
                .thenReturn(Optional.of(existingSession));
        when(refreshSessionRepository.revokeIfActive(existingSession.tokenHash(),
                Instant.parse("2026-07-20T10:00:00Z"))).thenReturn(false);

        RefreshTokenUseCase useCase = new RefreshTokenUseCase(refreshSessionRepository, userRepository,
                jwtTokenService, jwtProperties, clockProvider);

        assertThatThrownBy(() -> useCase.execute("raw-token")).isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsUnknownRefreshToken() {
        when(refreshSessionRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        RefreshTokenUseCase useCase = new RefreshTokenUseCase(refreshSessionRepository, userRepository,
                jwtTokenService, jwtProperties, clockProvider);

        assertThatThrownBy(() -> useCase.execute("unknown-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rejectsExpiredRefreshToken() {
        User user = User.reconstitute(UUID.randomUUID(), "ayse@example.com", "encoded-hash", UserRole.USER,
                Instant.parse("2026-01-01T00:00:00Z"));
        RefreshSession expiredSession = RefreshSession.issue(user.id(), RefreshTokenHasher.hash("raw-token"),
                Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-06-01T00:00:00Z"));
        when(refreshSessionRepository.findByTokenHash(RefreshTokenHasher.hash("raw-token")))
                .thenReturn(Optional.of(expiredSession));

        RefreshTokenUseCase useCase = new RefreshTokenUseCase(refreshSessionRepository, userRepository,
                jwtTokenService, jwtProperties, clockProvider);

        assertThatThrownBy(() -> useCase.execute("raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
