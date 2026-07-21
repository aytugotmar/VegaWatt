package com.vegawatt.core.auth.application;

import com.vegawatt.core.user.domain.User;
import java.time.Instant;

public record AuthSession(User user, String accessToken, String rawRefreshToken, Instant refreshTokenExpiresAt) {
}
