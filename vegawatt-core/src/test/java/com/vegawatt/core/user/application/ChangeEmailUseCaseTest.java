package com.vegawatt.core.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.session.domain.RefreshSessionRepository;
import com.vegawatt.core.user.domain.EmailAlreadyRegisteredException;
import com.vegawatt.core.user.domain.IncorrectPasswordException;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserRepository;
import com.vegawatt.core.user.domain.UserRole;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ChangeEmailUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-07-24T10:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshSessionRepository refreshSessionRepository;
    @Mock
    private ClockProvider clockProvider;

    private ChangeEmailUseCase useCase() {
        return new ChangeEmailUseCase(userRepository, passwordEncoder, refreshSessionRepository, clockProvider);
    }

    private static User existingUser(UUID id) {
        return User.reconstitute(id, "old@example.com", "hash", UserRole.USER, NOW);
    }

    @Test
    void normalizesAndChangesEmailAndRevokesAllSessionsOnSuccess() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-pass", "hash")).thenReturn(true);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(clockProvider.now()).thenReturn(NOW);

        useCase().execute(userId, "correct-pass", "  New@Example.com  ");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("new@example.com");
        verify(refreshSessionRepository).revokeAllByUserId(userId, NOW);
    }

    @Test
    void rejectsWrongCurrentPasswordWithoutCheckingEmailUniqueness() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> useCase().execute(userId, "wrong", "new@example.com"))
                .isInstanceOf(IncorrectPasswordException.class);

        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsAlreadyRegisteredEmail() {
        UUID userId = UUID.randomUUID();
        User user = existingUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-pass", "hash")).thenReturn(true);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> useCase().execute(userId, "correct-pass", "taken@example.com"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        verify(userRepository, never()).save(any());
        verify(refreshSessionRepository, never()).revokeAllByUserId(any(), any());
    }
}
