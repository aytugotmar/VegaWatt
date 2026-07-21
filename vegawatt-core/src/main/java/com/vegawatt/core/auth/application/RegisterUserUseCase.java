package com.vegawatt.core.auth.application;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.user.domain.EmailAlreadyRegisteredException;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserRepository;
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
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        User user = User.register(email, passwordEncoder.encode(rawPassword), clockProvider.now());
        return userRepository.save(user);
    }
}
