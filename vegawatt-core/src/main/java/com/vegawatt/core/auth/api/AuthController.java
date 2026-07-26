package com.vegawatt.core.auth.api;

import com.vegawatt.core.auth.application.AuthSession;
import com.vegawatt.core.auth.application.LoginUseCase;
import com.vegawatt.core.auth.application.LogoutUseCase;
import com.vegawatt.core.auth.application.RefreshTokenUseCase;
import com.vegawatt.core.auth.application.RegisterUserUseCase;
import com.vegawatt.core.auth.domain.InvalidRefreshTokenException;
import com.vegawatt.core.common.config.JwtProperties;
import com.vegawatt.core.common.config.RateLimitProperties;
import com.vegawatt.core.common.security.RefreshTokenHasher;
import com.vegawatt.core.user.domain.User;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final JwtProperties jwtProperties;
    private final RateLimitProperties rateLimitProperties;
    private final com.vegawatt.core.common.rate.RateLimiter rateLimiter;

    @org.springframework.beans.factory.annotation.Value("${vegawatt.auth.cookie-secure:false}")
    private boolean cookieSecure;

    AuthController(RegisterUserUseCase registerUserUseCase, LoginUseCase loginUseCase,
                    RefreshTokenUseCase refreshTokenUseCase, LogoutUseCase logoutUseCase,
                    JwtProperties jwtProperties, RateLimitProperties rateLimitProperties,
                    com.vegawatt.core.common.rate.RateLimiter rateLimiter) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.jwtProperties = jwtProperties;
        this.rateLimitProperties = rateLimitProperties;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterUserRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String clientKey = extractClientKey(httpRequest, request.email());
        rateLimiter.tryAcquire("register:" + clientKey, rateLimitProperties.registerPerMinute(), Duration.ofMinutes(1));

        User user = registerUserUseCase.execute(request.email(), request.password());
        AuthSession session = loginUseCase.issueSession(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session).toString())
                .body(AuthResponse.from(session));
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String clientKey = extractClientKey(httpRequest, request.email());
        rateLimiter.tryAcquire("login:" + clientKey, rateLimitProperties.loginPerMinute(), Duration.ofMinutes(1));

        AuthSession session = loginUseCase.execute(request.email(), request.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session).toString())
                .body(AuthResponse.from(session));
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
                                         jakarta.servlet.http.HttpServletRequest httpRequest) {
        // Keyed on the token itself (hashed, matching how it's stored), not IP + a hardcoded
        // literal — with nginx in front of core, every caller's getRemoteAddr() is the same
        // proxy IP, so an IP-based key would have collapsed every user's refresh calls into one
        // shared bucket, letting one heavy user (or attacker) exhaust everyone else's quota. Each
        // refresh token is already unique per session, so hashing it gives every user their own
        // bucket for free. Falls back to the IP-based key only when there's no token to key on.
        String clientKey = refreshToken != null ? RefreshTokenHasher.hash(refreshToken)
                : extractClientKey(httpRequest, "refresh");
        rateLimiter.tryAcquire("refresh:" + clientKey, rateLimitProperties.refreshPerMinute(), Duration.ofMinutes(1));

        if (refreshToken == null) {
            throw new InvalidRefreshTokenException();
        }
        AuthSession session = refreshTokenUseCase.execute(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session).toString())
                .body(AuthResponse.from(session));
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken != null) {
            logoutUseCase.execute(refreshToken);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearedRefreshCookie().toString())
                .build();
    }

    private ResponseCookie refreshCookie(AuthSession session) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, session.rawRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path(REFRESH_COOKIE_PATH)
                .sameSite("Lax")
                .maxAge(Duration.ofDays(jwtProperties.refreshTokenTtlDays()))
                .build();
    }

    private ResponseCookie clearedRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path(REFRESH_COOKIE_PATH)
                .sameSite("Lax")
                .maxAge(0)
                .build();
    }

    // Deliberately NOT reading X-Forwarded-For: nothing in this stack currently sits in front of
    // Spring Boot as a trusted reverse proxy, so that header is fully attacker-controlled and would
    // let anyone spoof their rate-limit key by sending a different value on every request. If a real
    // reverse proxy is added later, this needs a ForwardedHeaderFilter plus an explicit trusted-proxy
    // allowlist — not a blind header read.
    private String extractClientKey(jakarta.servlet.http.HttpServletRequest request, String identifier) {
        String ip = request.getRemoteAddr();
        return ip + ":" + (identifier != null ? identifier.toLowerCase().trim() : "anonymous");
    }
}
