package com.vegawatt.core.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.session.domain.RefreshSessionRepository;
import com.vegawatt.core.user.domain.LastAdminDemotionNotAllowedException;
import com.vegawatt.core.user.domain.SelfRoleChangeNotAllowedException;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserNotFoundException;
import com.vegawatt.core.user.domain.UserRepository;
import com.vegawatt.core.user.domain.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChangeUserRoleUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-07-24T10:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshSessionRepository refreshSessionRepository;
    @Mock
    private ClockProvider clockProvider;

    private ChangeUserRoleUseCase useCase() {
        return new ChangeUserRoleUseCase(userRepository, refreshSessionRepository, clockProvider);
    }

    private static User user(UUID id, UserRole role) {
        return User.reconstitute(id, id + "@example.com", "hash", role, NOW);
    }

    @Test
    void promotesUserToAdminAndRevokesTheirSessions() {
        UUID caller = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        when(userRepository.findById(target)).thenReturn(Optional.of(user(target, UserRole.USER)));
        when(clockProvider.now()).thenReturn(NOW);

        useCase().execute(caller, target, UserRole.ADMIN);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().role()).isEqualTo(UserRole.ADMIN);
        verify(refreshSessionRepository).revokeAllByUserId(target, NOW);
    }

    @Test
    void rejectsChangingOwnRole() {
        UUID caller = UUID.randomUUID();

        assertThatThrownBy(() -> useCase().execute(caller, caller, UserRole.USER))
                .isInstanceOf(SelfRoleChangeNotAllowedException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsDemotingTheLastRemainingAdmin() {
        UUID caller = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        when(userRepository.findById(target)).thenReturn(Optional.of(user(target, UserRole.ADMIN)));
        when(userRepository.findAll()).thenReturn(List.of(user(target, UserRole.ADMIN)));

        assertThatThrownBy(() -> useCase().execute(caller, target, UserRole.USER))
                .isInstanceOf(LastAdminDemotionNotAllowedException.class);

        verify(userRepository, never()).save(any());
        verify(refreshSessionRepository, never()).revokeAllByUserId(any(), any());
    }

    @Test
    void allowsDemotingAnAdminWhenAnotherAdminRemains() {
        UUID caller = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        UUID otherAdmin = UUID.randomUUID();
        when(userRepository.findById(target)).thenReturn(Optional.of(user(target, UserRole.ADMIN)));
        when(userRepository.findAll()).thenReturn(
                List.of(user(target, UserRole.ADMIN), user(otherAdmin, UserRole.ADMIN)));
        when(clockProvider.now()).thenReturn(NOW);

        useCase().execute(caller, target, UserRole.USER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().role()).isEqualTo(UserRole.USER);
    }

    @Test
    void rejectsUnknownTargetUser() {
        UUID caller = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        when(userRepository.findById(target)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(caller, target, UserRole.ADMIN))
                .isInstanceOf(UserNotFoundException.class);
    }
}
