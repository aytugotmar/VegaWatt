package com.vegawatt.core.auth.api;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vegawatt.core.auth.application.AuthSession;
import com.vegawatt.core.auth.application.LoginUseCase;
import com.vegawatt.core.auth.application.LogoutUseCase;
import com.vegawatt.core.auth.application.RefreshTokenUseCase;
import com.vegawatt.core.auth.application.RegisterUserUseCase;
import com.vegawatt.core.common.config.JwtProperties;
import com.vegawatt.core.common.config.RateLimitProperties;
import com.vegawatt.core.common.rate.RateLimitExceededException;
import com.vegawatt.core.common.rate.RateLimiter;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * With nginx in front of core, every caller's HttpServletRequest.getRemoteAddr() reports the same
 * proxy IP — so the /refresh endpoint's rate-limit key must NOT collapse to IP-only (or IP plus a
 * hardcoded literal), or one heavy user (or attacker) exhausts the shared bucket for every other
 * user. Uses a real RateLimiter (not a mock) so the assertion is on actual bucket-sharing behavior,
 * not on what string happens to get built.
 */
class AuthControllerTest {

    private final RegisterUserUseCase registerUserUseCase = mock(RegisterUserUseCase.class);
    private final LoginUseCase loginUseCase = mock(LoginUseCase.class);
    private final RefreshTokenUseCase refreshTokenUseCase = mock(RefreshTokenUseCase.class);
    private final LogoutUseCase logoutUseCase = mock(LogoutUseCase.class);
    private final JwtProperties jwtProperties = new JwtProperties("test-secret", 15, 30);
    private final RateLimiter rateLimiter = new RateLimiter();

    private AuthController controller(int refreshPerMinute) {
        RateLimitProperties rateLimitProperties = new RateLimitProperties(100, 100, refreshPerMinute, 100);
        return new AuthController(registerUserUseCase, loginUseCase, refreshTokenUseCase, logoutUseCase,
                jwtProperties, rateLimitProperties, rateLimiter);
    }

    private static AuthSession session(String refreshToken) {
        User user = User.reconstitute(UUID.randomUUID(), "user@example.com", "hash", UserRole.USER,
                Instant.parse("2026-01-01T00:00:00Z"));
        return new AuthSession(user, "access-token", refreshToken, Instant.now().plusSeconds(3600));
    }

    private static MockHttpServletRequest requestBehindSameProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.1");
        return request;
    }

    @Test
    void twoDifferentRefreshTokensFromTheSameProxyIpDoNotShareARateLimitBucket() {
        AuthController controller = controller(1);
        when(refreshTokenUseCase.execute("token-a")).thenReturn(session("token-a-rotated"));
        when(refreshTokenUseCase.execute("token-b")).thenReturn(session("token-b-rotated"));

        assertThatCode(() -> controller.refresh("token-a", requestBehindSameProxy())).doesNotThrowAnyException();
        // Different user, different refresh token, same proxy IP — must not be blocked by
        // token-a's already-exhausted quota of 1/minute.
        assertThatCode(() -> controller.refresh("token-b", requestBehindSameProxy())).doesNotThrowAnyException();
    }

    @Test
    void repeatingTheSameRefreshTokenHitsItsOwnRateLimit() {
        AuthController controller = controller(1);
        when(refreshTokenUseCase.execute("token-a")).thenReturn(session("token-a-rotated"));

        assertThatCode(() -> controller.refresh("token-a", requestBehindSameProxy())).doesNotThrowAnyException();

        assertThatThrownBy(() -> controller.refresh("token-a", requestBehindSameProxy()))
                .isInstanceOf(RateLimitExceededException.class);
    }
}
