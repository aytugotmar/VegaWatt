package com.vegawatt.core.user.infrastructure;

import com.vegawatt.core.common.config.BootstrapAdminProperties;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class BootstrapAdminRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final BootstrapAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClockProvider clockProvider;

    BootstrapAdminRunner(BootstrapAdminProperties properties, UserRepository userRepository,
                          PasswordEncoder passwordEncoder, ClockProvider clockProvider) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.clockProvider = clockProvider;
    }

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(properties.email()) || !StringUtils.hasText(properties.password())) {
            return;
        }

        Optional<User> existing = userRepository.findByEmail(properties.email());
        if (existing.isPresent() && !properties.forcePasswordReset()) {
            log.info("Bootstrap admin {} already exists; leaving its password untouched", properties.email());
            return;
        }

        User admin = existing
                .map(current -> User.reconstitute(current.id(), current.email(),
                        passwordEncoder.encode(properties.password()), current.role(), current.createdAt()))
                .orElseGet(() -> User.bootstrapAdmin(properties.email(),
                        passwordEncoder.encode(properties.password()), clockProvider.now()));
        userRepository.save(admin);
        log.info(existing.isPresent()
                ? "Force-reset bootstrap admin {} password (BOOTSTRAP_ADMIN_FORCE_PASSWORD_RESET=true)"
                : "Bootstrapped admin user {} with verified password hash", properties.email());
    }
}
