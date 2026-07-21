package com.vegawatt.core.auth.api;

import com.vegawatt.core.auth.application.AuthSession;
import com.vegawatt.core.auth.application.LoginUseCase;
import com.vegawatt.core.auth.application.LogoutUseCase;
import com.vegawatt.core.auth.application.RefreshTokenUseCase;
import com.vegawatt.core.auth.application.RegisterUserUseCase;
import com.vegawatt.core.auth.domain.InvalidRefreshTokenException;
import com.vegawatt.core.common.config.JwtProperties;
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

    AuthController(RegisterUserUseCase registerUserUseCase, LoginUseCase loginUseCase,
                    RefreshTokenUseCase refreshTokenUseCase, LogoutUseCase logoutUseCase,
                    JwtProperties jwtProperties) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        User user = registerUserUseCase.execute(request.email(), request.password());
        AuthSession session = loginUseCase.issueSession(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session).toString())
                .body(AuthResponse.from(session));
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthSession session = loginUseCase.execute(request.email(), request.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session).toString())
                .body(AuthResponse.from(session));
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
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
                .path(REFRESH_COOKIE_PATH)
                .sameSite("Lax")
                .maxAge(Duration.ofDays(jwtProperties.refreshTokenTtlDays()))
                .build();
    }

    private ResponseCookie clearedRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .path(REFRESH_COOKIE_PATH)
                .sameSite("Lax")
                .maxAge(0)
                .build();
    }
}
