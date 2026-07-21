package com.vegawatt.core.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.user.domain.EmailAlreadyRegisteredException;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserRepository;
import com.vegawatt.core.user.domain.UserRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ClockProvider clockProvider;

    @Test
    void registersUserWithEncodedPasswordAndUserRole() {
        when(userRepository.existsByEmail("ayse@example.com")).thenReturn(false);
        when(passwordEncoder.encode("s3cret-pass")).thenReturn("encoded-hash");
        when(clockProvider.now()).thenReturn(Instant.parse("2026-07-20T10:00:00Z"));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterUserUseCase useCase = new RegisterUserUseCase(userRepository, passwordEncoder, clockProvider);
        User result = useCase.execute("ayse@example.com", "s3cret-pass");

        assertThat(result.email()).isEqualTo("ayse@example.com");
        assertThat(result.passwordHash()).isEqualTo("encoded-hash");
        assertThat(result.role()).isEqualTo(UserRole.USER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void rejectsRegistrationWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("ayse@example.com")).thenReturn(true);

        RegisterUserUseCase useCase = new RegisterUserUseCase(userRepository, passwordEncoder, clockProvider);

        assertThatThrownBy(() -> useCase.execute("ayse@example.com", "s3cret-pass"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }
}
