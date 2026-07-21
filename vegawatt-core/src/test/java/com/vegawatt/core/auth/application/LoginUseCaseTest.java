package com.vegawatt.core.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.auth.domain.InvalidCredentialsException;
import com.vegawatt.core.common.config.JwtProperties;
import com.vegawatt.core.common.security.JwtTokenService;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LoginUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    @Test
    void issuesAccessAndRefreshTokensOnSuccessfulLogin() {
        JwtProperties jwtProperties = new JwtProperties("test-secret", 15, 30);
        ClockProvider clockProvider = () -> Instant.parse("2026-07-20T10:00:00Z");
        User user = User.reconstitute(UUID.randomUUID(), "ayse@example.com", "encoded-hash", UserRole.USER,
                Instant.parse("2026-01-01T00:00:00Z"));

        when(userRepository.findByEmail("ayse@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("s3cret-pass", "encoded-hash")).thenReturn(true);
        when(jwtTokenService.generateAccessToken(user.id(), UserRole.USER, Instant.parse("2026-07-20T10:00:00Z")))
                .thenReturn("access-token");
        when(refreshSessionRepository.save(any(RefreshSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoginUseCase useCase = new LoginUseCase(userRepository, passwordEncoder, jwtTokenService,
                refreshSessionRepository, jwtProperties, clockProvider);
        AuthSession session = useCase.execute("ayse@example.com", "s3cret-pass");

        assertThat(session.accessToken()).isEqualTo("access-token");
        assertThat(session.user()).isEqualTo(user);
        assertThat(session.rawRefreshToken()).isNotBlank();
        verify(refreshSessionRepository).save(any(RefreshSession.class));
    }

    @Test
    void rejectsLoginWhenUserDoesNotExist() {
        JwtProperties jwtProperties = new JwtProperties("test-secret", 15, 30);
        ClockProvider clockProvider = () -> Instant.parse("2026-07-20T10:00:00Z");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        LoginUseCase useCase = new LoginUseCase(userRepository, passwordEncoder, jwtTokenService,
                refreshSessionRepository, jwtProperties, clockProvider);

        assertThatThrownBy(() -> useCase.execute("missing@example.com", "whatever"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsLoginWhenPasswordDoesNotMatch() {
        JwtProperties jwtProperties = new JwtProperties("test-secret", 15, 30);
        ClockProvider clockProvider = () -> Instant.parse("2026-07-20T10:00:00Z");
        User user = User.reconstitute(UUID.randomUUID(), "ayse@example.com", "encoded-hash", UserRole.USER,
                Instant.parse("2026-01-01T00:00:00Z"));
        when(userRepository.findByEmail("ayse@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pass", "encoded-hash")).thenReturn(false);

        LoginUseCase useCase = new LoginUseCase(userRepository, passwordEncoder, jwtTokenService,
                refreshSessionRepository, jwtProperties, clockProvider);

        assertThatThrownBy(() -> useCase.execute("ayse@example.com", "wrong-pass"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
