package com.vegawatt.core.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vegawatt.core.common.security.RefreshTokenHasher;
import com.vegawatt.core.common.time.ClockProvider;
import com.vegawatt.core.session.domain.RefreshSession;
import com.vegawatt.core.session.domain.RefreshSessionRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    @Mock
    private RefreshSessionRepository refreshSessionRepository;

    private final ClockProvider clockProvider = () -> Instant.parse("2026-07-20T10:00:00Z");

    @Test
    void revokesMatchingSession() {
        RefreshSession session = RefreshSession.issue(UUID.randomUUID(), RefreshTokenHasher.hash("raw-token"),
                Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-07-01T00:00:00Z"));
        when(refreshSessionRepository.findByTokenHash(RefreshTokenHasher.hash("raw-token")))
                .thenReturn(Optional.of(session));

        LogoutUseCase useCase = new LogoutUseCase(refreshSessionRepository, clockProvider);
        useCase.execute("raw-token");

        assertThat(session.revokedAt()).isEqualTo(Instant.parse("2026-07-20T10:00:00Z"));
        verify(refreshSessionRepository).save(session);
    }

    @Test
    void doesNothingWhenTokenIsUnknown() {
        when(refreshSessionRepository.findByTokenHash(RefreshTokenHasher.hash("unknown"))).thenReturn(Optional.empty());

        LogoutUseCase useCase = new LogoutUseCase(refreshSessionRepository, clockProvider);
        useCase.execute("unknown");

        verify(refreshSessionRepository, never()).save(any());
    }
}
