package com.vegawatt.core.access.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.security.CurrentUser;
import com.vegawatt.core.home.domain.HomeNotFoundException;
import com.vegawatt.core.user.domain.UserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeAuthorizationServiceTest {

    @Mock
    private HomeAccessService homeAccessService;

    private HomeAuthorizationService service() {
        return new HomeAuthorizationService(homeAccessService);
    }

    @Test
    void allowsAMemberUserThroughWithoutThrowing() {
        UUID userId = UUID.randomUUID();
        UUID homeId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(userId, UserRole.USER);
        when(homeAccessService.canAccess(userId, homeId)).thenReturn(true);

        assertThatCode(() -> service().requireAccess(currentUser, homeId)).doesNotThrowAnyException();
    }

    @Test
    void rejectsANonMemberUserWithHomeNotFoundNotAForbidden() {
        UUID userId = UUID.randomUUID();
        UUID homeId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(userId, UserRole.USER);
        when(homeAccessService.canAccess(userId, homeId)).thenReturn(false);

        assertThatThrownBy(() -> service().requireAccess(currentUser, homeId))
                .isInstanceOf(HomeNotFoundException.class);
    }

    @Test
    void rejectsAccessToANonexistentHomeTheSameWayAsAHomeThatBelongsToSomeoneElse() {
        // HomeAuthorizationService never checks home existence directly — it only checks
        // membership — so a made-up homeId produces the exact same 404 as an unauthorized real
        // one. That's the deliberate anti-IDOR masking this class documents.
        UUID userId = UUID.randomUUID();
        UUID nonexistentHomeId = UUID.randomUUID();
        CurrentUser currentUser = new CurrentUser(userId, UserRole.USER);
        when(homeAccessService.canAccess(userId, nonexistentHomeId)).thenReturn(false);

        assertThatThrownBy(() -> service().requireAccess(currentUser, nonexistentHomeId))
                .isInstanceOf(HomeNotFoundException.class);
    }

    @Test
    void adminBypassesMembershipChecksEntirelyWithoutEvenQueryingThem() {
        UUID adminUserId = UUID.randomUUID();
        UUID homeId = UUID.randomUUID();
        CurrentUser adminUser = new CurrentUser(adminUserId, UserRole.ADMIN);

        assertThatCode(() -> service().requireAccess(adminUser, homeId)).doesNotThrowAnyException();
        verify(homeAccessService, never()).canAccess(adminUserId, homeId);
    }
}
