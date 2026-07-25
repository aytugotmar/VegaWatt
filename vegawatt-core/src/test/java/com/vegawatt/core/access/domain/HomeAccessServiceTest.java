package com.vegawatt.core.access.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HomeAccessServiceTest {

    @Mock
    private HomeMembershipRepository homeMembershipRepository;

    private HomeAccessService service() {
        return new HomeAccessService(homeMembershipRepository);
    }

    @Test
    void canAccessIsTrueWhenTheUserHasAMembershipForThatHome() {
        UUID userId = UUID.randomUUID();
        UUID homeId = UUID.randomUUID();
        when(homeMembershipRepository.existsByUserIdAndHomeId(userId, homeId)).thenReturn(true);

        assertThat(service().canAccess(userId, homeId)).isTrue();
    }

    @Test
    void canAccessIsFalseWhenTheUserHasNoMembershipForThatHome() {
        UUID userId = UUID.randomUUID();
        UUID homeId = UUID.randomUUID();
        when(homeMembershipRepository.existsByUserIdAndHomeId(userId, homeId)).thenReturn(false);

        assertThat(service().canAccess(userId, homeId)).isFalse();
    }

    @Test
    void canAccessIsFalseForANonexistentHome() {
        UUID userId = UUID.randomUUID();
        UUID nonexistentHomeId = UUID.randomUUID();
        when(homeMembershipRepository.existsByUserIdAndHomeId(userId, nonexistentHomeId)).thenReturn(false);

        assertThat(service().canAccess(userId, nonexistentHomeId)).isFalse();
    }

    @Test
    void canAccessDelegatesDirectlyToTheExistsQueryRatherThanLoadingTheFullMembershipList() {
        UUID userId = UUID.randomUUID();
        UUID homeId = UUID.randomUUID();
        when(homeMembershipRepository.existsByUserIdAndHomeId(userId, homeId)).thenReturn(true);

        assertThat(service().canAccess(userId, homeId)).isTrue();
        org.mockito.Mockito.verify(homeMembershipRepository, org.mockito.Mockito.never()).findHomeIdsByUser(userId);
    }
}
