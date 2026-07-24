package com.vegawatt.core.user.application;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.session.domain.RefreshSessionRepository;
import com.vegawatt.core.user.domain.IncorrectPasswordException;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserNotFoundException;
import com.vegawatt.core.user.domain.UserRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangePasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshSessionRepository refreshSessionRepository;
    private final ClockProvider clockProvider;

    public ChangePasswordUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                  RefreshSessionRepository refreshSessionRepository, ClockProvider clockProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshSessionRepository = refreshSessionRepository;
        this.clockProvider = clockProvider;
    }

    @Transactional
    public void execute(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new IncorrectPasswordException();
        }

        User updated = user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(updated);

        // A password change is exactly the moment a possibly-compromised session should stop
        // working, rather than staying valid until it naturally expires.
        refreshSessionRepository.revokeAllByUserId(userId, clockProvider.now());
    }
}
