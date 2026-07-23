package com.vegawatt.core.access.domain;

import com.vegawatt.core.common.security.CurrentUser;
import com.vegawatt.core.home.domain.HomeNotFoundException;
import com.vegawatt.core.user.domain.UserRole;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Shared home-scoped authorization gate for controllers under {@code /homes/{homeId}/...}.
 * Denied access surfaces as {@link HomeNotFoundException} (404), not 403, so an unauthorized
 * caller can't distinguish "not yours" from "doesn't exist".
 */
@Service
public class HomeAuthorizationService {

    private final HomeAccessService homeAccessService;

    public HomeAuthorizationService(HomeAccessService homeAccessService) {
        this.homeAccessService = homeAccessService;
    }

    public void requireAccess(CurrentUser currentUser, UUID homeId) {
        if (currentUser.role() != UserRole.ADMIN && !homeAccessService.canAccess(currentUser.userId(), homeId)) {
            throw new HomeNotFoundException(homeId);
        }
    }
}
