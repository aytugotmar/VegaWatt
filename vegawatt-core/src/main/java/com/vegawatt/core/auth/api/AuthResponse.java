package com.vegawatt.core.auth.api;

import com.vegawatt.core.auth.application.AuthSession;
import java.util.UUID;

public record AuthResponse(UUID userId, String email, String role, String accessToken) {

    public static AuthResponse from(AuthSession session) {
        return new AuthResponse(session.user().id(), session.user().email(), session.user().role().name(),
                session.accessToken());
    }
}
