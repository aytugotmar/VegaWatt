package com.vegawatt.core.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.vegawatt.core.common.config.JwtProperties;
import com.vegawatt.core.user.domain.UserRole;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private final JwtProperties jwtProperties = new JwtProperties(
            "unit-test-signing-secret-of-at-least-256-bits-long", 15, 30);
    private final JwtTokenService jwtTokenService = new JwtTokenService(jwtProperties);

    @Test
    void roundTripsUserIdAndRoleThroughAccessToken() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        String token = jwtTokenService.generateAccessToken(userId, UserRole.ADMIN, now);
        Optional<CurrentUser> parsed = jwtTokenService.parseAccessToken(token);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().userId()).isEqualTo(userId);
        assertThat(parsed.get().role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void rejectsGarbageToken() {
        assertThat(jwtTokenService.parseAccessToken("not-a-jwt")).isEmpty();
    }

    @Test
    void rejectsExpiredToken() {
        UUID userId = UUID.randomUUID();
        Instant issuedAt = Instant.now().minusSeconds(60 * 60);

        String token = jwtTokenService.generateAccessToken(userId, UserRole.USER, issuedAt);

        assertThat(jwtTokenService.parseAccessToken(token)).isEmpty();
    }
}
