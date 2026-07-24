package com.vegawatt.core.user.application;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.session.domain.RefreshSessionRepository;
import com.vegawatt.core.user.domain.EmailAlreadyRegisteredException;
import com.vegawatt.core.user.domain.IncorrectPasswordException;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserNotFoundException;
import com.vegawatt.core.user.domain.UserRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeEmailUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshSessionRepository refreshSessionRepository;
    private final ClockProvider clockProvider;

    public ChangeEmailUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder,
                               RefreshSessionRepository refreshSessionRepository, ClockProvider clockProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshSessionRepository = refreshSessionRepository;
        this.clockProvider = clockProvider;
    }

    @Transactional
    public User execute(UUID userId, String currentPassword, String newEmail) {
        String normalizedEmail = newEmail.trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new IncorrectPasswordException();
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }

        User updated = user.changeEmail(normalizedEmail);
        userRepository.save(updated);
        refreshSessionRepository.revokeAllByUserId(userId, clockProvider.now());
        return updated;
    }
}
