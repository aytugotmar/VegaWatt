package com.vegawatt.core.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.session.domain.RefreshSessionRepository;
import com.vegawatt.core.user.domain.IncorrectPasswordException;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserNotFoundException;
import com.vegawatt.core.user.domain.UserRepository;
import com.vegawatt.core.user.domain.UserRole;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ChangePasswordUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-07-24T10:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshSessionRepository refreshSessionRepository;
    @Mock
    private ClockProvider clockProvider;

    private ChangePasswordUseCase useCase() {
        return new ChangePasswordUseCase(userRepository, passwordEncoder, refreshSessionRepository, clockProvider);
    }

    private static User existingUser(UUID id) {
        return User.reconstitute(id, "ayse@example.com", "old-hash", UserRole.USER, NOW);
    }

    @Test
    void changesPasswordAndRevokesAllSessionsOnSuccess() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-pass", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("new-hash");
        when(clockProvider.now()).thenReturn(NOW);

        useCase().execute(userId, "old-pass", "new-pass");

        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().passwordHash()).isEqualTo("new-hash");
        verify(refreshSessionRepository).revokeAllByUserId(userId, NOW);
    }

    @Test
    void rejectsWrongCurrentPasswordWithoutSavingOrRevoking() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> useCase().execute(userId, "wrong", "new-pass"))
                .isInstanceOf(IncorrectPasswordException.class);

        verify(userRepository, never()).save(any());
        verify(refreshSessionRepository, never()).revokeAllByUserId(any(), any());
    }

    @Test
    void rejectsUnknownUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase().execute(userId, "old-pass", "new-pass"))
                .isInstanceOf(UserNotFoundException.class);
    }
}
