package com.vegawatt.core.user.application;

import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.session.domain.RefreshSessionRepository;
import com.vegawatt.core.user.domain.LastAdminDemotionNotAllowedException;
import com.vegawatt.core.user.domain.SelfRoleChangeNotAllowedException;
import com.vegawatt.core.user.domain.User;
import com.vegawatt.core.user.domain.UserNotFoundException;
import com.vegawatt.core.user.domain.UserRepository;
import com.vegawatt.core.user.domain.UserRole;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeUserRoleUseCase {

    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final ClockProvider clockProvider;

    public ChangeUserRoleUseCase(UserRepository userRepository, RefreshSessionRepository refreshSessionRepository,
                                  ClockProvider clockProvider) {
        this.userRepository = userRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.clockProvider = clockProvider;
    }

    @Transactional
    public void execute(UUID callerUserId, UUID targetUserId, UserRole newRole) {
        if (targetUserId.equals(callerUserId)) {
            throw new SelfRoleChangeNotAllowedException();
        }

        User target = userRepository.findById(targetUserId).orElseThrow(() -> new UserNotFoundException(targetUserId));

        if (target.role() == UserRole.ADMIN && newRole != UserRole.ADMIN && remainingAdminCount() <= 1) {
            throw new LastAdminDemotionNotAllowedException();
        }

        User updated = target.changeRole(newRole);
        userRepository.save(updated);

        // A role change (e.g. demoting an admin) should invalidate that user's existing sessions —
        // otherwise a JWT issued minutes ago keeps carrying the old role until it expires.
        refreshSessionRepository.revokeAllByUserId(targetUserId, clockProvider.now());
    }

    private int remainingAdminCount() {
        return userRepository.countAdminsUnderGlobalLock();
    }
}
