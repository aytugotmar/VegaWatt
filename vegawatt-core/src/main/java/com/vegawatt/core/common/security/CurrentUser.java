package com.vegawatt.core.common.security;

import com.vegawatt.core.user.domain.UserRole;
import java.util.UUID;

public record CurrentUser(UUID userId, UserRole role) {
}
