package com.vegawatt.core.auth.application;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.user.domain.EmailAlreadyRegisteredException;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClockProvider clockProvider;

    public RegisterUserUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                ClockProvider clockProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clockProvider = clockProvider;
    }

    @Transactional
    public User execute(String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyRegisteredException(normalizedEmail);
        }
        User user = User.register(normalizedEmail, passwordEncoder.encode(rawPassword), clockProvider.now());
        return userRepository.save(user);
    }
}
