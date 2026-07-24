package com.vegawatt.core.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.config.BootstrapAdminProperties;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserRepository;
import com.vegawatt.core.user.domain.UserRole;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminRunnerTest {

    private static final Instant NOW = Instant.parse("2026-07-24T10:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ClockProvider clockProvider;

    private BootstrapAdminRunner runner(BootstrapAdminProperties properties) {
        return new BootstrapAdminRunner(properties, userRepository, passwordEncoder, clockProvider);
    }

    @Test
    void createsAdminWhenNoneExists() {
        BootstrapAdminProperties properties = new BootstrapAdminProperties("admin@vegawatt.com", "pw", false);
        when(userRepository.findByEmail("admin@vegawatt.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("hash");
        when(clockProvider.now()).thenReturn(NOW);

        runner(properties).run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("admin@vegawatt.com");
        assertThat(captor.getValue().passwordHash()).isEqualTo("hash");
    }

    @Test
    void leavesExistingAdminPasswordUntouchedByDefault() {
        BootstrapAdminProperties properties = new BootstrapAdminProperties("admin@vegawatt.com", "pw", false);
        User existing = User.reconstitute(java.util.UUID.randomUUID(), "admin@vegawatt.com", "already-set-hash",
                UserRole.ADMIN, NOW);
        when(userRepository.findByEmail("admin@vegawatt.com")).thenReturn(Optional.of(existing));

        runner(properties).run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void forcePasswordResetOverwritesExistingAdminPassword() {
        BootstrapAdminProperties properties = new BootstrapAdminProperties("admin@vegawatt.com", "pw", true);
        User existing = User.reconstitute(java.util.UUID.randomUUID(), "admin@vegawatt.com", "already-set-hash",
                UserRole.ADMIN, NOW);
        when(userRepository.findByEmail("admin@vegawatt.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("pw")).thenReturn("forced-hash");

        runner(properties).run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().passwordHash()).isEqualTo("forced-hash");
    }

    @Test
    void doesNothingWhenPasswordIsBlank() {
        BootstrapAdminProperties properties = new BootstrapAdminProperties("admin@vegawatt.com", "", false);

        runner(properties).run();

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }
}
